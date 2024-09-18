# RIPE Whois Database

Specific
-------
1.修改代码使其适配特定的数据格式

2.删减businessRule认证

3.增加md5密码生成脚本(python)

4.修改domain类型的存储与索引


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
source ./who is-commons/src/main/resources/who is_schema.sql 
use MAILUPDATES_LOCAL;
source ./who is-commons/src/main/resources/mailupdates_schema.sql 
use ACL_LOCAL;
source ./who is-commons/src/main/resources/acl_schema.sql 
use INTERNALS_LOCAL;
source ./who is-commons/src/main/resources/internals_schema.sql

use NRTM_LOCAL;
source ./who is-commons/src/main/resources/nrtm_schema.sql 
use WHOIS_LOCAL;
source ./who is-commons/src/main/resources/who is_data.sql 
use INTERNALS_LOCAL;
source ./who is-commons/src/main/resources/internals_data.sql 
use NRTM_LOCAL;
source ./who is-commons/src/main/resources/nrtm_data.sql
```

切换dbase用户
```
//启动whois，注意看报错
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
安装maildrop
下载地址：https://sourceforge.net/projects/courier/files/maildrop/

wget https://sourceforge.net/projects/courier/files/maildrop/3.1.8/maildrop-3.1.8.tar.bz2/download
tar xvfj maildrop-3.1.8.tar.bz2
sudo apt-get install g++ libpcre3-dev
groupadd maildrop
sudo groupadd maildrop
sudo useradd -g maildrop maildrop
cd maildrop-3.1.8/
sudo apt install libpcre2-dev
id maildrop
./configure --enable-maildrop-uid=1004 --enable-maildrop-gid=1004  --enable-trusted-users='root qmaild maildrop'


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
From mail1@test-cernet.com  Thu Sep 12 02:36:39 2024\nReturn-Path: <mail1@test-cernet.com>\nReceived: from test-cernet.com (test-cernet.com [1.51.2.207])\nby test-cernet.com (8.18.1/8.18.1/Debian-2) with SMTP id 48C2YPYl007825\nfor dbase@test-cernet.com; Thu, 12 Sep 2024 02:35:07 GMT\nDate: Thu, 12 Sep 2024 02:34:25 GMT\nFrom: mail1@test-cernet.com\nMessage-Id: <202409120235.48C2YPYl007825@test-cernet.com>\nSUBJECT: NEW\ninetnum: 182.16.6.0 - 182.16.6.255\nnetname: NL-RIPENCC-TCA6-20120101\nconn-i: BJ000003\norg: ORG-CO1-TEST\ndescr: RIPE NCC Training Services Attendee\nremarks: Allocation used in the training courses\ncountry: EU\nadmin-c: GWY-TEST\ntech-c: GWY-TEST\nstatus: ALLOCATED PA\nmnt-by: TEST-DBM-MNT\nsource: TEST\npassword: Cernet@572\n\n
```
```
Date: Mon, 29 Jun 2009 18:39:03 +0800
From: "=?gb2312?B?26zQocHB?=" <gaoxl@legendsec.com>
To: "moreorless" <moreorless@live.cn>
Cc: "gxl0620" <gxl0620@163.com>
BCC: "=?gb2312?B?26zQocHB?=" <venus.oso@gmail.com>
Subject: attach
Message-ID: <200906291839032504254@legendsec.com>
X-mailer: Foxmail 6, 15, 201, 21 [cn]
Mime-Version: 1.0
```


### MD5 password generate example
```
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
