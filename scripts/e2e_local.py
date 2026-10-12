"""Real local E2E: isolated PostgreSQL/Kafka, all four jars, installed Ollama.
Run after mvn package -DskipTests. Never uses production DBs or sends Email.
"""
from pathlib import Path
import base64, hashlib, hmac, json, os, secrets, socket, subprocess, time, urllib.request, uuid

root = Path(__file__).resolve().parents[1]
run_id = 'helpdesk-e2e-' + uuid.uuid4().hex[:12]
logs = root/'target'/'e2e'/run_id
logs.mkdir(parents=True)
env = os.environ.copy()
def port():
    with socket.socket() as s:
        s.bind(('127.0.0.1',0)); return s.getsockname()[1]
pg_port,kafka_port=port(),port()
env.update(E2E_PG_PORT=str(pg_port),E2E_KAFKA_PORT=str(kafka_port),E2E_DB_PASSWORD=secrets.token_urlsafe(24))
docker=['docker']
if os.name=='nt': docker += ['--host','npipe:////./pipe/docker_engine']
compose=docker+['compose','-p',run_id,'-f',str(root/'docker-compose.e2e.yml')]
processes=[]; files=[]
def command(args,**kwargs):
    return subprocess.run(args,check=True,env=env,cwd=root,**kwargs)
def request(url,method='GET',body=None,token=None,headers=None,timeout=180):
    h={'Content-Type':'application/json',**(headers or {})}
    if token: h['Authorization']='Bearer '+token
    req=urllib.request.Request(url,data=None if body is None else json.dumps(body).encode(),method=method,headers=h)
    with urllib.request.urlopen(req,timeout=timeout) as response:
        content=response.read();return json.loads(content) if content else None
def wait_for(action,seconds,label):
    deadline=time.monotonic()+seconds;last=None;progress=0
    while time.monotonic()<deadline:
        try:
            result=action()
            if result:return result
        except Exception as exc:last=type(exc).__name__
        if time.monotonic()>progress:print(label,flush=True);progress=time.monotonic()+15
        time.sleep(2)
    raise RuntimeError(label+' timed out; last error '+str(last))
secret=secrets.token_urlsafe(48);internal=secrets.token_urlsafe(32)
def jwt(user,role):
    def b64(value):return base64.urlsafe_b64encode(value).rstrip(b'=')
    header=b64(b'{"alg":"HS256","typ":"JWT"}')
    payload=b64(json.dumps({'sub':str(user),'role':role,'email':'admin@example.test','type':'access','exp':int(time.time())+1200}).encode())
    data=header+b'.'+payload
    return (data+b'.'+b64(hmac.new(secret.encode(),data,hashlib.sha256).digest())).decode()
ports={module:port() for module in ['api-gateway','knowledge-base-service','notification-service','ai-worker']}
dbs={'api-gateway':'gateway','knowledge-base-service':'knowledge_base','notification-service':'notification','ai-worker':'ai_worker'}
try:
    models=request(os.getenv('AI_BASE_URL','http://localhost:11434')+'/api/tags')
    for required in [os.getenv('AI_MODEL','qwen3:4b'),os.getenv('AI_EMBEDDING_MODEL','nomic-embed-text')]:
        if not any(m['name']==required or m['name'].split(':')[0]==required for m in models['models']):
            raise RuntimeError('Install Ollama model first: '+required)
    command(compose+['up','-d','--wait','--wait-timeout','180'])
    for db in dbs.values():command(compose+['exec','-T','postgres','psql','-U','helpdesk','-d','postgres','-c','CREATE DATABASE '+db])
    for module in ports:
        jar=root/module/'target'/f'{module}-1.0.0-SNAPSHOT.jar'
        if not jar.exists():raise RuntimeError('Build executable jars first: mvn package -DskipTests')
        flags=[f'--server.port={ports[module]}','--server.address=127.0.0.1',
            f'--spring.datasource.url=jdbc:postgresql://127.0.0.1:{pg_port}/{dbs[module]}',
            '--spring.datasource.username=helpdesk',f'--spring.datasource.password={env["E2E_DB_PASSWORD"]}',
            f'--spring.kafka.bootstrap-servers=127.0.0.1:{kafka_port}',f'--jwt.secret={secret}',
            f'--internal.api-key={internal}',f'--api-gateway.api-key={internal}',f'--knowledge-base.api-key={internal}',
            f'--api-gateway.base-url=http://127.0.0.1:{ports["api-gateway"]}',
            f'--knowledge-base.base-url=http://127.0.0.1:{ports["knowledge-base-service"]}',
            '--notification.email.enabled=false','--logging.level.org.hibernate.SQL=warn',
            '--management.health.mail.enabled=false','--spring.main.banner-mode=off',
            '--spring.kafka.consumer.properties.max.poll.interval.ms=900000']
        out=(logs/(module+'.log')).open('wb');files.append(out)
        processes.append(subprocess.Popen(['java','-Xmx256m','-jar',str(jar),*flags],stdout=out,stderr=subprocess.STDOUT,cwd=root,env=env))
    base={m:f'http://127.0.0.1:{p}' for m,p in ports.items()}
    for module in ports:wait_for(lambda m=module:request(base[m]+'/actuator/health',timeout=3).get('status')=='UP',120,'Waiting for '+module)
    article='Чтобы сбросить пароль, откройте страницу входа, нажмите «Забыли пароль», укажите email и следуйте инструкции в письме.'
    saved=request(base['knowledge-base-service']+'/api/v1/articles','POST',{'title':'Сброс пароля','content':article},jwt(uuid.uuid4(),'ADMIN'))
    search=request(base['knowledge-base-service']+'/internal/v1/knowledge/search','POST',{'query':article,'topK':5},headers={'X-Internal-Api-Key':internal})
    assert search['chunks'] and search['chunks'][0]['articleId']==saved['id']
    assert search['chunks'][0]['score']>=0.75
    email='e2e-'+uuid.uuid4().hex+'@example.test';password=secrets.token_urlsafe(24)
    request(base['api-gateway']+'/api/v1/auth/register','POST',{'email':email,'password':password,'fullName':'Synthetic E2E User'})
    login=request(base['api-gateway']+'/api/v1/auth/login','POST',{'email':email,'password':password})
    token=login['accessToken']
    ticket=request(base['api-gateway']+'/api/v1/tickets','POST',{'message':article},token)
    def completed():
        value=request(base['api-gateway']+'/api/v1/tickets/'+ticket['id'],token=token,timeout=5)
        return value if value.get('generatedAnswer') and value['status'] in ['ANSWED','ANSWERED','WAITIND_OPERATOR','WAITING_OPERATOR'] else None
    result=wait_for(completed,600,'Waiting for real Kafka + Ollama ticket processing')
    assert result['status'] in ['ANSWED','ANSWERED'],result
    def notified():
        items=request(base['notification-service']+'/api/v1/notifications',token=token)['content']
        return next((i for i in items if i['ticketId']==ticket['id']),None)
    notification=wait_for(notified,60,'Waiting for notification persistence')
    request(base['notification-service']+'/api/v1/notifications/'+notification['id']+'/delivered','POST',token=token)
    # With an empty knowledge base the worker must escalate, not invent an automatic answer.
    request(base['knowledge-base-service']+'/api/v1/articles/'+saved['id'],'DELETE',token=jwt(uuid.uuid4(),'ADMIN'))
    fallback_ticket=request(base['api-gateway']+'/api/v1/tickets','POST',{'message':'I cannot sign in to my account. Please help.'},token)
    def escalated():
        value=request(base['api-gateway']+'/api/v1/tickets/'+fallback_ticket['id'],token=token,timeout=5)
        return value if value.get('generatedAnswer') and value['status'] in ['WAITIND_OPERATOR','WAITING_OPERATOR'] else None
    fallback=wait_for(escalated,600,'Waiting for empty-KB human escalation')
    def fallback_notified():
        items=request(base['notification-service']+'/api/v1/notifications',token=token)['content']
        return next((i for i in items if i['ticketId']==fallback_ticket['id']),None)
    fallback_notification=wait_for(fallback_notified,60,'Waiting for escalation notification')
    assert 'оператор' in fallback_notification['content']
    report={'success':True,'articleId':saved['id'],'ticketId':ticket['id'],'notificationId':notification['id'],
            'status':result['status'],'fallbackStatus':fallback['status'],'fallbackNotificationId':fallback_notification['id'],'ragScore':search['chunks'][0]['score'],'answerLength':len(result['generatedAnswer'])}
    (logs/'result.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
    print(json.dumps(report),flush=True)
finally:
    for process in processes:
        if process.poll() is None:process.terminate()
    for process in processes:
        try:process.wait(timeout=15)
        except subprocess.TimeoutExpired:process.kill();process.wait()
    for stream in files:stream.close()
    # Only this unique test project is removed; existing project containers and volumes are untouched.
    subprocess.run(compose+['down','-v','--remove-orphans'],env=env,cwd=root,check=False)
    print('Test logs: '+str(logs),flush=True)
