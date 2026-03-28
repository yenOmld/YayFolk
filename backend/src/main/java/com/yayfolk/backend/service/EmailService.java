package com.yayfolk.backend.service;

import com.yayfolk.backend.utils.VerificationCodeUtil;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;
import org.springframework.core.env.Environment;

import java.util.concurrent.TimeUnit;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final RedisTemplate<String, String> redisTemplate;
    private final Environment environment;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final String VERIFICATION_CODE_PREFIX = "verification_code:";
    private static final long EXPIRATION_MINUTES = 5;

    @Autowired
    public EmailService(JavaMailSender mailSender, RedisTemplate<String, String> redisTemplate, Environment environment) {
        this.mailSender = mailSender;
        this.redisTemplate = redisTemplate;
        this.environment = environment;
    }

    public void sendVerificationCode(String toEmail) {
        String code = VerificationCodeUtil.generateCode();
        String key = VERIFICATION_CODE_PREFIX + toEmail;
        
        redisTemplate.opsForValue().set(key, code, EXPIRATION_MINUTES, TimeUnit.MINUTES);

        String pwd = environment.getProperty("spring.mail.password");
        boolean hasCreds = fromEmail != null && !fromEmail.trim().isEmpty() && pwd != null && !pwd.trim().isEmpty();
        if (!hasCreds) {
            System.out.println("邮件未配置或密码缺失，跳过实际发送");
            return;
        }

        MimeMessagePreparator preparator = new MimeMessagePreparator() {
            public void prepare(MimeMessage mimeMessage) throws Exception {
                mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
                mimeMessage.setFrom(new InternetAddress(fromEmail, "YayFolk", "UTF-8"));
                mimeMessage.setSubject("YayFolk 验证码");
                mimeMessage.setText("您好，欢迎使用 YayFolk，很高兴成为您探索非遗文化的伙伴！\n\n您的验证码是: " + code + "\n\n该验证码5分钟内有效，请勿泄露给他人。");
            }
        };

        try {
            this.mailSender.send(preparator);
        } catch (MailException ex) {
            System.err.println("发送邮件失败: " + ex.getMessage());
        }
    }

    public boolean verifyCode(String email, String code) {
        String key = VERIFICATION_CODE_PREFIX + email;
        String storedCode = redisTemplate.opsForValue().get(key);
        
        if (storedCode == null) {
            return false;
        }
        
        if (!storedCode.equals(code)) {
            return false;
        }
        
        redisTemplate.delete(key);
        return true;
    }
}
