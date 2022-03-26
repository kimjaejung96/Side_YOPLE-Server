package com.map.mutual.side.auth.svc.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.map.mutual.side.auth.model.dto.UserInfoDto;
import com.map.mutual.side.auth.model.entity.JWTRefreshTokenLogEntity;
import com.map.mutual.side.auth.model.entity.UserEntity;
import com.map.mutual.side.auth.repository.JWTRepo;
import com.map.mutual.side.auth.repository.UserInfoRepo;
import com.map.mutual.side.common.JwtTokenProvider;
import com.map.mutual.side.common.enumerate.ApiStatusCode;
import com.map.mutual.side.common.exception.YOPLEServiceException;
import com.map.mutual.side.auth.constant.SmsConstant;
import com.map.mutual.side.auth.model.entity.SMSRequestLogEntity;
import com.map.mutual.side.auth.model.dto.SMSAuthReqeustDto;
//import com.map.mutual.side.auth.model.dto.MessageInfoDto;
import com.map.mutual.side.auth.model.dto.SmsDto;
import com.map.mutual.side.auth.repository.SMSLogRepo;
import com.map.mutual.side.auth.svc.AuthService;
import com.map.mutual.side.auth.utils.HttpSensClient;
import lombok.extern.log4j.Log4j2;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;

/**
 * Class       : SmsServiceImpl
 * Author      : 김 재 중
 * Description : Class Description
 * History     : [2022-03-13] - 조준희 - smsAuthNum 저장 서비스 생성,
 */
@Service
@Log4j2
public class AuthServiceImpl implements AuthService {

    private SMSLogRepo smsLogRepo;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final UserInfoRepo userInfoRepo;
    private final JWTRepo jwtRepo;
    private ModelMapper modelMapper;

    @Autowired
    public AuthServiceImpl(SMSLogRepo smsLogRepo,
                           JwtTokenProvider tokenProvider,
                           AuthenticationManagerBuilder authenticationManagerBuilder,
                           UserInfoRepo userInfoRepo,
                           JWTRepo jwtRepo,
                           ModelMapper modelMapper) {
        this.smsLogRepo = smsLogRepo;
        this.tokenProvider = tokenProvider;
        this.authenticationManagerBuilder = authenticationManagerBuilder;
        this.userInfoRepo = userInfoRepo;
        this.jwtRepo = jwtRepo;
        this.modelMapper = modelMapper;
    }


    public void sendMessageTest(String sendPhoneNum, String smsAuthNum) throws IOException {
        int resultCode = 0;

        String hostNameUrl = SmsConstant.SENS_HOST_URL;
        String requestUrl= SmsConstant.SENS_REQUEST_URL;
        String requestUrlType = SmsConstant.SENS_REQUEST_TYPE;
        String sensAccessKey = SmsConstant.SENS_ACCESSKEY;
        String serviceId = SmsConstant.SENS_SVC_ID;

        String sensApiUrl = requestUrl + serviceId + requestUrlType;
        String timeStamp = Long.toString(System.currentTimeMillis());
        String apiUrl = hostNameUrl + sensApiUrl;

        SmsDto smsDto  = SmsDto.builder()
                .type(SmsConstant.SENS_MESSAGE_TYPE_SMS)
                .contentType(SmsConstant.SENS_MESSAGE_CONTENTTPYE_COMM)
                .countryCode(SmsConstant.SENS_MESSAGE_COUNTRYCODE_DEFAULT)
                .from("01055967356")
//                .subject("SMS")
                .content("[인증]")
//                .content("기본 콘텐츠" + Integer.toString(RandomUtils.nextInt(10000, 100000)))
                .messages(Collections
                        .singletonList(SmsDto.MessageInfoDto.builder()
                                .to(sendPhoneNum)
                                .content("인증번호를 입력하세요 [" + smsAuthNum + "]")
                                .build()))
                .build();


        ObjectMapper mapper = new ObjectMapper();

        String jsonStr = mapper.writeValueAsString(smsDto);



        StringEntity stringEntity = new StringEntity(jsonStr, "UTF-8");

        try{
            HttpClient httpClient = HttpSensClient.getHttpClientInsecure();
            HttpPost httpPost = new HttpPost(apiUrl);
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-Type", "application/json; charset=utf-8");
            httpPost.addHeader("Connection", "keep-alive");
            httpPost.addHeader("x-ncp-apigw-timestamp", timeStamp);
            httpPost.addHeader("x-ncp-iam-access-key", sensAccessKey);
            httpPost.addHeader("x-ncp-apigw-signature-v2", HttpSensClient.makeSignature(timeStamp, sensApiUrl));


            httpPost.setEntity(stringEntity);

            HttpResponse httpResponse = httpClient.execute(httpPost);
            resultCode = httpResponse.getStatusLine().getStatusCode();

        } catch (Exception e) {
            log.error("Error : {}", e.getMessage());
        }
        log.info(resultCode);
    }

    @Override
    public void smsAuthNumSave(SMSAuthReqeustDto smsAuthReqeustDTO, String smsAuthNum) {

        try {

            // 2. SMSRequestLog 생성
            SMSRequestLogEntity smsLog = SMSRequestLogEntity.builder().phone(smsAuthReqeustDTO.getPhone())
                    .requestAuthNum(smsAuthNum)
                    .duid(smsAuthReqeustDTO.getUuid())
                    .build();

            // 3. Log 저장
            smsLogRepo.save(smsLog);

            log.debug("SMS Auth Number Insert Success !! ");
        }
        catch(Exception e)
        {
            log.error("SMS Auth Number Insert Failed!! : %s",e.getMessage());
            throw e;
        }

    }

    @Override
    public void smsAuthNumResponse(SMSAuthReqeustDto smsAuthResponseDTO) throws RuntimeException{

        try {
            SMSRequestLogEntity smslog = smsLogRepo
                    .findTop1ByPhoneAndCreateTimeBetweenOrderByCreateTime(
                            smsAuthResponseDTO.getPhone(),
                            LocalDateTime.now().minusMinutes(5),
                            LocalDateTime.now());
            if (smslog == null) {
                throw new RuntimeException();
            }

            // 인증 코드 확인
            if(smslog.getRequestAuthNum().equals(smsAuthResponseDTO.getResponseAuthNum()))
            {
                // match
                smslog.setResponseAuthNum(smsAuthResponseDTO.getResponseAuthNum());
                smsLogRepo.save(smslog);
                log.debug("SMS Auth Number \"smsAuthNumResponse\" Update!!");
            }
            else
            {
                log.debug("SMSAuthNum Not Match!!");
                throw new YOPLEServiceException(ApiStatusCode.AUTH_META_NOT_MATCH);
            }

        }catch(RuntimeException e)
        {
            log.error("smsAuthNumber Response Failed : %s",e.getMessage());
            throw e;
        }

    }

    @Override
    public UserInfoDto signUp(UserInfoDto user) throws Exception {
        UserEntity userEntity = modelMapper.map(user, UserEntity.class);
        try {
            userInfoRepo.save(userEntity);
            return user;
        } catch (Exception e) {
            log.error("SignUp Error occured : {}", e.getMessage());
            throw e;
        }
    }
    public UserEntity findOneByPhone(String phone){
        try{
            return userInfoRepo.findOneByPhone(phone);
        }catch(Exception e) {
            throw e;
        }
    }

    @Override
    public String JWTAccessRefresh(String refreshToken) throws Exception {

        try {
            //Refresh 벨리데이션 + 유효기간 체크.
            if (tokenProvider.validateToken(refreshToken) == false)
                throw new YOPLEServiceException(ApiStatusCode.UNAUTHORIZED);

            // 액세스 토큰 갱신
            String suid = ((UserInfoDto)(tokenProvider.getAccessAuthentication(refreshToken).getPrincipal())).getSuid();

            // JWT 리플레시 로그 불러옴.
            JWTRefreshTokenLogEntity jwtRefreshTokenLogEntity = jwtRepo.findOneByUserSuid(suid);

            // DB 저장된 리플레시와 요청으로 받은 리플레시가 다를 경우 Exception
            if (jwtRefreshTokenLogEntity.getRefreshToken().equals(refreshToken) == false)
                throw new YOPLEServiceException(ApiStatusCode.UNAUTHORIZED);



            UserInfoDto userInfoDto = UserInfoDto.builder().suid(suid).build();
            String accessToken = makeAccessJWT(userInfoDto);

            return accessToken;

        }catch(Exception e)
        {
            throw e;
        }
    }

    @Override
    public void saveJwtLog(JWTRefreshTokenLogEntity log) throws Exception {
        try {
            jwtRepo.save(log);
        }catch(Exception e)
        {
            throw e;
        }
    }

    public String makeAccessJWT(UserInfoDto user) throws Exception {
        Authentication authentication = null ;
        String jwt = "";

        try {
            authentication =  new UsernamePasswordAuthenticationToken(user,null);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            jwt = tokenProvider.createAccessTokenFromAuthentication(authentication);
        }catch(AuthenticationException ex)  // 인증 절차 실패시 리턴되는 Exception
        {
            //logger.debug("AuthController Auth 체크 실패 "+ ex.getMessage());
            throw ex;
        }catch(Exception ex)
        {
            //logger.error("AuthController Exception : " + ex.getMessage());
            throw ex;
        }   // 체크 필요!

        return jwt;
    }

    public String makeRefreshJWT(String suid) throws Exception {
        String jwt;
        try {
            jwt = tokenProvider.createRefreshTokenFromAuthentication(suid);
        }catch(AuthenticationException ex)  // 인증 절차 실패시 리턴되는 Exception
        {
            //logger.debug("AuthController Auth 체크 실패 "+ ex.getMessage());
            throw ex;
        }catch(Exception ex)
        {
            //logger.error("AuthController Exception : " + ex.getMessage());
            throw ex;
        }   // 체크 필요!

        return jwt;
    }

}
