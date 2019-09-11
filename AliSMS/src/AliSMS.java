import java.io.FileInputStream;
import java.util.Properties;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
public class AliSMS {
	Properties key = new Properties();
	AliSMS(String f){
		try {
			key.load(new FileInputStream(f));
		} catch (Exception e) {
			System.out.println("Can not open file: "+f);
		}
	}
	public static void main(String[] args) {
		AliSMS mysms = new AliSMS("conf.properties");
		String nos = "13100000000,15900000000";
		String sign = "卓豪中国";
		String temp = "SMS_17270000";
		String param = "{\"message\":\"测试一下下\"}";
		if(args.length<4) {
			System.out.println("Usage:\njava -cp .;lib/* AliSMS <PhoneNumbers> <SignName> <TemplateCode> <TemplateParam>");
			System.exit(20148);
		}

		nos=args[0];
		sign=args[1];
		temp=args[2];
		StringBuffer sb = new StringBuffer();
		for(int i=3; i<args.length;i++) {
			sb.append(args[i]);
		}
		param=sb.toString();	
		mysms.sendSMS(nos, sign, temp, param);
	}
    public void sendSMS(String nos, String sign, String temp, String param) {
    	String keyId = key.getProperty("AccessKeyId", null);
    	String keySecret =  key.getProperty("AccessKeySecret", null);
    	if(null==keyId ||keySecret==null) {
    		System.out.println("Please set AccessKey values in conf.properties file");
    		System.exit(1024);
    	}
    	DefaultProfile profile = DefaultProfile.getProfile("default", keyId, keySecret);
        IAcsClient client = new DefaultAcsClient(profile);

        CommonRequest request = new CommonRequest();
        request.setMethod(MethodType.POST);
        request.setDomain("dysmsapi.aliyuncs.com");
        request.setVersion("2017-05-25");
        request.setAction("SendSms");
        request.putQueryParameter("RegionId", "default");
        request.putQueryParameter("PhoneNumbers", nos);
        request.putQueryParameter("SignName", sign);
        request.putQueryParameter("TemplateCode", temp);
        request.putQueryParameter("TemplateParam", param);
        try {
            CommonResponse response = client.getCommonResponse(request);
            System.out.println(response.getData());
        } catch (ServerException e) {
            e.printStackTrace();
        } catch (ClientException e) {
            e.printStackTrace();
        }
    }
}
