package com.smarthelpdesk.apigateway.servise;
import com.smarthelpdesk.apigateway.support.DatabaseTestSupport;
import com.smarthelpdesk.apigateway.service.*;
import com.smarthelpdesk.apigateway.repository.*;
import com.smarthelpdesk.apigateway.exception.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest
class AuthDatabaseIntegrationTest extends DatabaseTestSupport {
 @Autowired AuthService auth; @Autowired RefreshTokenService refresh;
 @Autowired UserRepository users; @Autowired JdbcTemplate jdbc;
 String account(){String email=UUID.randomUUID()+"@example.test";auth.register(email,"Strong-password-1","Test User");return email;}
 @Test void passwordAndRefreshAreNeverStoredAsPlainText(){
  String email=account();var login=auth.login(email,"Strong-password-1");
  assertThat(users.findByEmail(email).orElseThrow().getPasswordHash()).doesNotContain("Strong-password-1");
  assertThat(jdbc.queryForObject("SELECT count(*) FROM refresh_token WHERE token_hash=?",Long.class,login.refreshToken())).isZero();
  assertThat(refresh.validate(login.refreshToken())).isNotNull();
 }
 @Test void rotationRevokesOldTokenAndLogoutRevokesNewOne(){
  var login=auth.login(account(),"Strong-password-1");var rotated=auth.refresh(login.refreshToken());
  assertThat(rotated.refreshToken()).isNotEqualTo(login.refreshToken());
  assertThatThrownBy(()->auth.refresh(login.refreshToken())).isInstanceOf(InvalidRefreshTokenException.class);
  auth.logout(rotated.refreshToken());assertThatThrownBy(()->auth.refresh(rotated.refreshToken())).isInstanceOf(InvalidRefreshTokenException.class);
 }
 @Test void expiredRefreshCannotRotate(){
  var login=auth.login(account(),"Strong-password-1");var token=refresh.validate(login.refreshToken());
  jdbc.update("UPDATE refresh_token SET expires_at=now()-interval '1 second' WHERE id=?",token.getId());
  assertThatThrownBy(()->auth.refresh(login.refreshToken())).isInstanceOf(InvalidRefreshTokenException.class);
 }
 @Test void concurrentRotationHasExactlyOneWinner() throws Exception {
  var login=auth.login(account(),"Strong-password-1");var start=new CountDownLatch(1);
  try(var pool=Executors.newFixedThreadPool(6)) {
   List<Future<Boolean>> results=new ArrayList<>();
   for(int i=0;i<6;i++)results.add(pool.submit(()->{start.await();try{auth.refresh(login.refreshToken());return true;}catch(InvalidRefreshTokenException ex){return false;}}));
   start.countDown();int successes=0;for(var f:results)if(f.get(15,TimeUnit.SECONDS))successes++;
   assertThat(successes).isEqualTo(1);
  }
 }
 @Test void wrongPasswordAndDuplicateAccountAreRejected(){
  String email=account();assertThatThrownBy(()->auth.login(email,"wrong")).isInstanceOf(InvalidCredentialsException.class);
  assertThatThrownBy(()->auth.register(email,"another","Name")).isInstanceOf(EmailAlreadyExistsException.class);
 }
 @Autowired UserService userService;
 @Test void realUserServiceHasRepositoryInjected(){String email=account();var user=users.findByEmail(email).orElseThrow();assertThat(userService.getById(user.getId()).getEmail()).isEqualTo(email);assertThat(userService.getByEmail(email).getId()).isEqualTo(user.getId());}
}
