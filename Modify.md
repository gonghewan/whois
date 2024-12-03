# RIPE Whois Database

Specific
-------
1.修改代码使其适配特定的数据格式

2.删减businessRule认证

3.增加md5密码生成脚本(python)

4.修改domain类型的存储与索引

5.额外搭建一个邮件服务器用于接收email-update请求

6.增加nserver类型，并实现相关项索引


环境配置
-----------------------------------------

参考官方文档： [Building whois | Docs 环境配置](https://apps.db.ripe.net/docs/Installation-and-Development/Building-whois/)

//安装java (一定要执行这个下载，不然会有报错，可能是javahome不对)

sudo apt install openjdk-21-jdk

//查看/切换java版本

sudo update-alternatives --config java 

//安装git

sudo apt-get install git

//配置ssh公钥，添加至github 

//安装maven

sudo apt-get install maven 

//安装mariadb

sudo apt-get install mariadb-client-5.5 mariadb-server-5.5 

//安装后会要求重启，查看进程启动终端会打印mariadbd相关信息

ps -ef | grep mysql

//登录mysql，不需要使用密码，可能是安装时自动配置了auth 

sudo mysql -u root

//新建用户 dbase:dbasedbase

useradd dbase passwd dbase

//check pgrep是否已安装

//安装sendmail

sudo apt-get install sendmail

//下载命令行运行工具，使用参考https://docs.cyclopsgroup.org/jmxterm
https://github.com/jiaqi/jmxterm/releases/download/v1.0.4/jmxterm-1.0.4-uber.jar

cd /home/dbase && sudo curl -O https://github.com/jiaqi/jmxterm/releases/download/v1.0.4/

whois安装与运行
-------------------------------------

git clone git@git hub.com:RIPE-NCC/whois.git cd whois

参考[安装手册](https://apps.db.ripe.net/docs/Installation-and-Development/Installation-instructions/)

```
// 编译产出一版whois-db，产出路径在whois-db/target/xx(pom.xml中的version名).jar
mvn clean install -Prelease

// 将产出包移动至dbase的家目录下 
sudo mkdir /home/dbase
sudo mv whois-db/target/whois-db-1.0-SNAPSHOT.jar /home/dbase/whois.jar 
sudo cp tools/hazel cast.xml /home/dbase/
sudo cp whois-commons/src/test/resources/log4j2.xml /home/dbase/
sudo cp whois-commons/src/test/resources/whois.properties /home/dbase/properties 
sudo chown dbase:dbase -R /home/dbase/
```

```
//修改/etc/mysql/mysql.conf.d/mysqld.cnf //添加以下两行
max_allowed_packet = 20M
innodb_buffer_pool_size = 2G
sudo service mysql restart   
//创建whois需要的用户
CREATE USER 'dbint '@ 'localhost ' IDENTIFIED BY ' ';   
GRANT ALL PRIVILEGES ON *.* TO 'dbint '@ 'localhost ';
CREATE USER 'rdonly '@ 'localhost ' IDENTIFIED BY ' ';
GRANT SELECT PRIVILEGES ON *.* TO 'rdonly '@ 'localhost ';
// 创建表
sudo mysql -u root
CREATE CREATE CREATE CREATE CREATE
DATABASE DATABASE DATABASE DATABASE DATABASE
WHOIS_LOCAL;
MAILUPDATES_LOCAL; ACL_LOCAL;
INTERNALS_LOCAL; NRTM_LOCAL;

// 写入test数据  
use WHOIS_LOCAL;
source ./whois-commons/src/main/resources/whois_schema.sql 
use MAILUPDATES_LOCAL;
source ./whois-commons/src/main/resources/mailupdates_schema.sql 
use ACL_LOCAL;
source ./whois-commons/src/main/resources/acl_schema.sql 
use INTERNALS_LOCAL;
source ./whois-commons/src/main/resources/internals_schema.sql

use NRTM_LOCAL;
source ./whois-commons/src/main/resources/nrtm_schema.sql 
use WHOIS_LOCAL;
source ./whois-commons/src/main/resources/whois_data.sql 
use INTERNALS_LOCAL;
source ./whois-commons/src/main/resources/internals_data.sql 
use NRTM_LOCAL;
source ./whois-commons/src/main/resources/nrtm_data.sql
```

切换dbase用户
```
//启动whois，注意看报错 nrtm目前有报错
/usr/bin/java -Dwhois -Djsse.enableSNIExtension=false -Dcom.sun.management.jmxremote -Dhazelcast.jmx=true -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=1099 -Xms1024m -Xmx8g -Dwhois.config=properties -Duser.timezone=UTC -Dhazelcast.config=hazelcast.xml -Dlog4j.configurationFile=file:log4j2.xml -jar whois.jar
//验证一下
telnet localhost 1043
```

```
//测试db数据
sudo cp /home/gwy/Whois/whois/whois-scheduler/src/test/resources/TEST.db /home/dbase/

//用vscode直接拖拽上传jar，因为ubuntuserver下载总是失败

su dbase
//获取pid
pgrep java

java --add-exports jdk.jconsole/sun.tools.jconsole=ALL-UNNAMED -jar jmxterm-1.0.4-uber.jar
open (PID HERE)
bean net.ripe.db.whois:name=Bootstrap
run loadDump comment TEST.db
//看到220 succeed 数据加载成功
```

### Templates
[Domain Template](http://1.51.2.207:1080/whois/metadata/templates/domain)

### Request with SyncUpdates

See [Request with SyncUpdates](https://docs.db.ripe.net/Update-Methods/Syncupdates/)
```
[file templete]
----------------------------466af99a9520
Content-Disposition: form-data; name="DATA"

person:         TEST GWY
nic-hdl:        GWY-TEST
address:        Singel 258, 1016 AB Amsterdam
phone:          +31205354444
e-mail:         gonghewan@gmail.com
remarks:        ********************************
remarks:        This Object is only an example!
remarks:        ********************************
source:         TEST
mnt-by:         TEST-DBM-MNT
password:       yourpassword
----------------------------466af99a9520--
```
```
增(POST && NEW=yes)
curl -v -4 -X POST -H "Content-Type: multipart/form-data; boundary=--------------------------466af99a9520" --data-binary @create_person.txt  http://localhost:1080/whois/syncupdates/test?NEW=yes

删（POST && DELETE=yes）
curl -v -4 -X POST -H "Content-Type: multipart/form-data; boundary=--------------------------466af99a9520" --data-binary @create_mnt.txt  http://localhost:1080/whois/syncupdates/test?Delete=yes

改（POST）
curl -v -4 -X POST -H "Content-Type: multipart/form-data; boundary=--------------------------466af99a9520" --data-binary @create_person.txt  http://localhost:1080/whois/syncupdates/test
```

### Request with Rest
- See [Installation instructions](https://apps.db.ripe.net/docs/Installation-and-Development/Installation-instructions).
  
```
[file templete]
{
"objects": {
    "object": [
    {
        "source": {
        "id": "TEST"
        },
        "attributes": {
        "attribute": [
            {
            "name": "person",
            "value": "TEST GWY"
            },
            {
            "name": "address",
            "value": "Singel 258"
            },
            {
            "name": "phone",
            "value": "+31-1234567890"
            },
            {
            "name": "mnt-by",
            "value": "TEST-DBM-MNT"
            },
            {
            "name": "nic-hdl",
            "value": "GWY1-TEST"
            },
            {
            "name": "remarks",
            "value": "remark"
            },
            {
                "name": "e-mail",
                "value": "gonghewan@gmail.com"
            },
            {
            "name": "source",
            "value": "TEST"
            }
        ]
        }
    }
    ]
}
}
```
```
查：
curl -X GET -H 'Content-Type: application/json' 'http://localhost:1080/whois/test/Person/GWY-TEST'

增:
curl -X POST -H 'Content-Type: application/json' --data @create_person_json.txt 'http://localhost:1080/whois/test/person?password=xxx'

改：
curl -X PUT -H 'Content-Type: application/json' --data @create_person_json.txt 'http://localhost:1080/whois/test/person?password=xxx'

删：
curl -v -4 -X DELETE -H 'Content-Type: application/json' -H 'Accept:application/json' --data @create_person_json.txt  http://localhost:1080/whois/test/person/GWY-TEST?password=xxx

```

### Request with Search
```
http://ip:port/whois/search?query-string=0.0.0.0
http://1.51.2.207:1080/whois/search?query-string=ns.ripe.net&inverse-attribute=nserver //inverse search
```

### Request with mail
[ubuntu配置mail server](https://blog.csdn.net/m0_56363537/article/details/127962135)
实际上只有一种，就是列在文件/etc/mail/relay-domains,默认安装后无此文件，你可以创建它：
abc.com (/etc/mail/relay-domains)
abc.com relay (/etc/mail/access)

sudo service sendmail restart //启动不起来看/var/log/mail.err

```
依次执行：
telnet hostname 25
mail from:"hello"<mail1@test-cernet.com>
rcpt to:dbase@test-cernet.com
data
hello world!
.
邮件投送成功输出提示：
250 2.0.0 48C1xH2S003505 Message accepted for delivery

可以在/var/mail/mail1看到mail
```

```
(安装maildrop)[https://www.yunweiku.com/thread-206994-1-1.html]

https://ftp.debian.org/debian/pool/main/libu/libunistring/
下载并安装libunistring2_1.0-2_amd64.deb 和 libunistring-dev
sudo dpkg -i ./libunistring2_1.0-2_amd64.deb
apt-cache policy libunistring2

wget http://mirrors.kernel.org/ubuntu/pool/main/libi/libidn2/libidn2-0_2.2.0-2_amd64.deb
wget http://mirrors.kernel.org/ubuntu/pool/main/libi/libidn2/libidn2-0-dev_2.2.0-2_amd64.deb 
wget http://archive.ubuntu.com/ubuntu/pool/main/libi/libidn2/libidn2-dev_2.2.0-2_amd64.deb
sudo dpkg -i libidn2-dev_2.2.0-2_amd64.deb
sudo apt-mark hold libidn2-dev
sudo dpkg -i libidn2-0-dev_2.2.0-2_amd64.deb 
sudo apt-mark hold libidn2-0-dev
sudo dpkg -i libidn2-0_2.2.0-2_amd64.deb
sudo apt-mark hold libidn2-0

wget https://sourceforge.net/projects/courier/files/courier-unicode/2.1/courier-unicode-2.1.tar.bz2/download
mv download courier-unicode-2.1.tar.bz2
tar xivf courier-unicode-2.1.tar.bz2
cd courier-unicode-2.1
./configure
make
make install

sudo apt-get install libcourier-unicode4

//下载地址：https://sourceforge.net/projects/courier/files/maildrop/
wget https://sourceforge.net/projects/courier/files/maildrop/3.1.8/maildrop-3.1.8.tar.bz2/download
tar xvfj maildrop-3.1.8.tar.bz2
sudo apt-get install g++ libpcre3-dev
groupadd maildrop
sudo groupadd maildrop
sudo useradd -g maildrop maildrop
cd maildrop-3.1.8/
sudo apt install libpcre2-dev
id maildrop
./configure --enable-maildrop-uid=1004 --enable-maildrop-gid=1004 --enable-trusted-users='root qmaild maildrop dbase gwy'
make
sudo make install
maildrop -v
chmod 4755 /usr/local/bin/maildrop
```
#### 邮件服务器搭建
```
//Postfix是一个邮件传输代理软件，用于电子邮件的收发过程
//Dovecot是一个IMAP和POP3邮件服务器

sudo apt-update
sudo apt-get install -y postfix dovecot-imapd dovecot-pop3d
//暂时选择Satellite system模式
//编辑配置文件
sudo vim /etc/dovecot/dovecot.conf
//增加一行
protocols = imap pop3 lmtp

sudo apt install dovecot-lmtpd
sudo systemctl restart postfix dovecot
sudo apt install mailutils

//测试一下
telnet localhost 110 # POP3
telnet localhost 143 # IMAP
echo "This is a test message" | mail -s 'test email' mail1@test-cernet.com

//配置maildrop
参考：http://www.panticz.de/index.php/node/154

```

/home/gwy/whois/whois/whois-api/src/main/java/net/ripe/db/whois/api/mail/dequeue/MessageDequeue.java line197 mailMessageDao.getMessage(messageId);
+
/home/gwy/whois/whois/whois-api/src/main/java/net/ripe/db/whois/api/mail/dao/MailMessageDao.java
+
/home/gwy/whois/whois/whois-api/src/main/java/net/ripe/db/whois/api/mail/dao/MailMessageDaoJdbc.java

email格式解析：
/home/gwy/whois/whois/whois-api/src/main/java/net/ripe/db/whois/api/mail/dequeue/BouncedMessageParser.java

/home/gwy/whois/whois/whois-api/src/main/java/net/ripe/db/whois/api/UpdateCreator.java
+
/home/gwy/whois/whois/whois-update/src/main/java/net/ripe/db/whois/update/domain/UpdateContext.java addmessage向mysql中插入message

```
# email更新格式，注意在每条更新数据间隔两个空行（包括首行）
telnet test-cernet.com smtp
Trying 1.51.2.207...
Connected to test-cernet.com.
Escape character is '^]'.
220 test-cernet.com ESMTP Postfix (Ubuntu)
helo test-cernet.com
250 test-cernet.com
mail from: gwy@test-cernet.com
250 2.1.0 Ok
rcpt to: dbase@test-cernet.com
250 2.1.5 Ok
data
354 End data with <CR><LF>.<CR><LF>
From: gwy <gwy@test-cernet.com>
To: dbase@test-cernet.com
Subject: NEW


password: ********
inetnum: 182.16.6.0 - 182.16.6.255
netname: NL-RIPENCC-TCA6-20120101
conni: BJ00000009
org: ORG-CO2-TEST
descr:          IPv4 address space
country:        CN # Country is really world wide
admin-c:        GWY-TEST
tech-c:         GWY-TEST
status:         ALLOCATED UNSPECIFIED
remarks:        The country is really worldwide.
mnt-by:         TEST-DBM-MNT
remarks:        This is an automatically created object.
source:         TEST
.
250 2.0.0 Ok: queued as EFB3F1206BA
quit
221 2.0.0 Bye
```
```
Escape character is '^]'.
220 test-cernet.com ESMTP Postfix (Ubuntu)
helo test-cernet.com
250 test-cernet.com
mail from: gwy@test-cernet.com
250 2.1.0 Ok
rcpt to: dbase@test-cernet.com
250 2.1.5 Ok
data
354 End data with <CR><LF>.<CR><LF>
From: gwy <gwy@test-cernet.com>
To: dbase@test-cernet.com
Subject: delete inetnum


inetnum: 182.16.6.0 - 182.16.6.255
source:         TEST
delete:  no longer need
password: ******
.
250 2.0.0 Ok: queued as C05F91212EC
quit
221 2.0.0 Bye
```
```
Trying 1.51.2.207...
Connected to test-cernet.com.
Escape character is '^]'.
220 test-cernet.com ESMTP Postfix (Ubuntu)
helo test-cernet.com
250 test-cernet.com
mail from: gwy@test-cernet.com
250 2.1.0 Ok
rcpt to: dbase@test-cernet.com
250 2.1.5 Ok
data
354 End data with <CR><LF>.<CR><LF>
From: gwy <gwy@test-cernet.com>
To: dbase@test-cernet.com
Subject: update my objects


inetnum:        182.16.7.0 - 182.16.7.255
netname:        NL-RIPENCC-TCA6-20120102
conni:          BJ000006
org:            ORG-CO1-TEST
descr:          The whole IPv4 address space
country:        CN # Country is really world wide
admin-c:        GWY-TEST
tech-c:         GWY-TEST
status:         ALLOCATED UNSPECIFIED
remarks:        The country is really worldwide.
mnt-by:         TEST-DBM-MNT
mnt-routes:     TEST-DBM-MNT
remarks:        This is test for email-updates
source:         TEST
password:       ******
.
250 2.0.0 Ok: queued as 862131212EC
quit
221 2.0.0 Bye
```

### MD5 password generate example
```
# python2
import crypt
password = 'emptypassword'
salt = '$1$N2zhyJ3g$'
print(crypt.crypt(password, salt))

output:
$1$N2zhyJ3g$hzX7XTL84DtBkCWhBZE2c/
```
* First install the AspectJ plugin provided by JetBrains from the IDE plugin repository
* Go to Build, Execution, Deployment -> Compiler -> Java Compiler
  * Choose "Use Compiler: Ajc"
  * Configure Path to aspectjtools.jar, e.g. ~/.m2/repository/org/aspectj/aspectjtools/1.9.8/aspectjtools-1.9.7.jar
  * Press "Test" to confirm it's working.
* Go to Build, Execution, Deployment -> Build Tools -> Maven -> Importing
  * Uncheck "Detect compiler automatically" (otherwise IntelliJ will revert from Ajc to JavaC)

### 开启HTTPS
选择PKCS#1或者PKCS#8，修改/home/gwy/whois/whois/whois-api/src/main/java/net/ripe/db/whois/api/httpserver/WhoisKeystore.java，重新编译，修改properties
```
# Service ports
# HTTPS
whois.private.keys=/home/dbase/tmp_server_tra.key
whois.certificates=/home/dbase/tmp_server.crt
whois.keystore=/home/dbase/cerbot/server.jks
port.api.secure=0
```

### 增加nserver
```
修改原nserver属性
原search逻辑：
/home/gwy/whois/whois/whois-api/src/main/java/net/ripe/db/whois/api/rest/WhoisSearchService.java @Path("/search")

/home/gwy/whois/whois/whois-api/src/main/java/net/ripe/db/whois/api/rest/RpslObjectStreamer.java queryHandler.streamResults

/home/gwy/whois/whois/whois-query/src/main/java/net/ripe/db/whois/query/handler/QueryHandler.java queryExecutor.execute

/home/gwy/whois/whois/whois-query/src/main/java/net/ripe/db/whois/query/executor/SearchQueryExecutor.java rpslObjectSearcher.search

/home/gwy/whois/whois/whois-query/src/main/java/net/ripe/db/whois/query/executor/RpslObjectSearcher.java line133 executeForObjectType
修改：
1.增加NSERVER：
/home/gwy/whois/whois/whois-rpsl/src/main/java/net/ripe/db/whois/common/rpsl/ObjectType.java
public enum ObjectType   yes
/home/gwy/whois/whois/whois-commons/src/main/java/net/ripe/db/whois/common/dao/jdbc/domain/ObjectTypeIds.java 增加nserver对应的typeid yes

2./home/gwy/whois/whois/whois-query/src/main/java/net/ripe/db/whois/query/query/Query.java
增加ObjectType.NSERVER：
    private static final EnumSet<ObjectType> GRS_LIMIT_TYPES = EnumSet.of(ObjectType.AUT_NUM, ObjectType.INETNUM, ObjectType.INET6NUM, ObjectType.ROUTE, ObjectType.ROUTE6, ObjectType.DOMAIN);
    private static final EnumSet<ObjectType> DEFAULT_TYPES_LOOKUP_IN_BOTH_DIRECTIONS = EnumSet.of(ObjectType.INETNUM, ObjectType.INET6NUM, ObjectType.ROUTE, ObjectType.ROUTE6, ObjectType.DOMAIN);
    private static final EnumSet<ObjectType> DEFAULT_TYPES_ALL = EnumSet.allOf(ObjectType.class);
yes
3./home/gwy/whois/whois/whois-query/src/main/java/net/ripe/db/whois/query/executor/RpslObjectSearcher.java
不做改动，由private Iterable<ResponseObject> indexLookup需要做如下4改动
由result.add(rpslObjectDao.findByKey(type, searchValue))需要做如下5改动

4./home/gwy/whois/whois/whois-rpsl/src/main/java/net/ripe/db/whois/common/rpsl/ObjectTemplate.java
修改domain的nserver字段为lookupkey属性/keyarrtribute属性，参考conni no

5. /home/gwy/whois/whois/whois-commons/src/main/java/net/ripe/db/whois/common/dao/jdbc/JdbcRpslObjectDao.java
类似
/home/gwy/whois/whois/whois-commons/src/main/java/net/ripe/db/whois/common/dao/jdbc/index/IndexWithConni.java
实现IndexWithNServer.java

6.从@Path("/syncupdates")看新增nserver的逻辑
/home/gwy/whois/whois/whois-commons/src/main/java/net/ripe/db/whois/common/dao/jdbc/JdbcRpslObjectUpdateDao.java line215 先更新index再更新last，新增时先更新last再更新index
在/home/gwy/whois/whois/whois-commons/src/main/java/net/ripe/db/whois/common/dao/jdbc/JdbcRpslObjectOperations.java中实现更新，逻辑不需要改
nserver表已经有了，将host字段设置为只能填域名AttributeSyntax，需要新增nameserver表，
/home/gwy/whois/whois/whois-rpsl/src/main/java/net/ripe/db/whois/common/rpsl/attrs/NServer.java yes
```

### 路由类 route

/home/gwy/whois/whois/whois-client/src/main/java/net/ripe/db/whois/api/rest/client/RestClientTarget.java