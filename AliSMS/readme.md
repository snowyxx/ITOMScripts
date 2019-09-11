## Usage

Usage:

`java -cp .;bin;lib/* AliSMS <PhoneNumbers> <SignName> <TemplateCode> <TemplateParam>`

__prerequisite__ : set the AccessKey information in conf.properties file

- PhoneNumbers: mobile numbers, split by comma, e.g. 13141410000,15910260000
- SignName: set in Ali console
- TemplateCode: get in Ali console
- TemplateParam: it is dependent on the template you set in Ali console.

__Refer__

[发送短信的步骤](https://help.aliyun.com/document_detail/108066.html)
[AccessKey的设置](https://help.aliyun.com/document_detail/101339.html)