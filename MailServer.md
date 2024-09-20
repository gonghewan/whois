# 邮件服务器搭建

## 邮件服务组成

- MUA （Mail User Agent） 邮件用户代理
  
  用户与电子邮件系统的接口，在大多数情况下就是在邮件客户端上运行的程序。常见MUA：使用最广泛的基于网页来实现的webmail，其他的有Outlook，Foxmail，thunderbird（雷鸟），Evolution，mutt（基于字符界面）

- MTA （Mail Transfer Agent）邮件传输代理
  
  主要功能发送和接收邮件，同时向发件人报告邮件的传送情况。根据用途可将邮件服务器分为邮件服务器（SMTP服务器）和接受邮件服务器（POP3/IMAP4服务器）。常用Exchange，lotus Notes Domino （内部使用），互联网上使用较多的软件sendmail（较老），postfix，Qmail（短小精悍）

- MDA（Mail  Delivery  Agent）邮件投递代理
  
  到本地邮局之后，将邮件放在用户的邮箱中。可以是MTA下面的一个小程序，也可以单独是一个软件例如procmail，maildrop

- MAA（Mail Access  Agent）邮件访问代理
  
  代为MUA提供访问message store并接收邮件功能的一台IMAP/POP3服务器，常用courrier-imap ，dovecot

- SASL（Simple Authorization  Secure Layer）简单认证安全层协议
  
  用于为没有提供认证功能的协议提供认证功能。软件包：cyus-sasl，进程：saslauthd

- 邮箱类型
  
  - mbox  所有邮件放在同一个文件中，新邮件直接追加在文件后面
  
  - maildir 建一个目录，每一封邮件被当成一个单独文件存放在目录里

        redhat默认使用mbox方式，sendmail默认使用mbox，postfix默认两种都支持

- SMIME
  
  提供端到端的邮件加密解密协议。

**postfix**作为MTA(Mail Transfer Agent), 负责创建smtp服务（smtpd）接收本域用户或其他域名服务器投递来的邮件，负责向其他服务器投递（转发）邮件，管理邮件队列

**maildrop**作为MDA(Mail Delivery Agent)负责把从postfix接收到邮件存入邮件夹，同时还支持自动转发、自动回复，邮件过滤等功能

**courier-authlib** 为 maildrop 提供与用户相关的信息查询

**dovecot**提供系统的POP3和IMAP服务，同时给postfix提供SMTP的SASL认证服务。

#### 电子邮件使用的协议

- SMTP (Simple Mail Transmission Protocol)，简单邮件传输协议 ，监听tcp 25号端口

- POP3 (Post Office Protocol)，邮局协议 ，监听tcp 110端口

- IMAP4(Internet Mail Access Protocol)，互联网邮件访问协议，监听tcp 143端口

- MIME(Multipurpose Internet Mail Extension)，多用途、多功能互联网邮件扩展：以文本的方式对二进制数据做重新编码，并能够实现以文本协议发送二进制数据，常用编码编码方式：base64

#### Postfix工作原理

[PostfixBasicSetupHowto](https://help.ubuntu.com/community/PostfixBasicSetupHowto)

## 参考

https://neoserver.site/help/step-step-installation-instructions-postfix-and-dovecot-ubuntu

[Install and configure Postfix - Ubuntu Server documentation](https://documentation.ubuntu.com/server/how-to/mail-services/install-postfix/?_ga=2.209831317.1681305109.1726653012-1834928379.1726653012&_gl=1*rsb1r9*_gcl_au*MTE5NzczMzQ3OC4xNzI2NjUzMDE5)

## 环境

Ubuntu20.04

MariDB 10.11.8

```shell
# 卸载已经安装的服务
sudo apt purge postfix
systemctl status postfix
# output: Unit postfix.service could not be found.
```

## 软件包列表

- libunistring2,libunistring-dev

- libidn2-dev_2.2.0-2, libidn2-0-dev_2.2.0-2,libidn2-0_2.2.0-2

- courier-unicode-2.1

- libcourier-unicode4

- pcre

- postfix

- maildrop

## SSL

参考[About certificates - Ubuntu Server documentation](https://documentation.ubuntu.com/server/explanation/security/certificates/#certificates)

```shell
# 生成证书和公私钥, pass phrase: 20240731
openssl genrsa -des3 -out server.key 2048

openssl rsa -in server.key -out server.key.insecure
mv server.key server.key.secure
mv server.key.insecure server.key

#Generate a Certificate Signing Request (CSR)
openssl req -new -key server.key -out server.csr
########################################################################
You are about to be asked to enter information that will be incorporated
into your certificate request.
What you are about to enter is what is called a Distinguished Name or a DN.
There are quite a few fields but you can leave some blank
For some fields there will be a default value,
If you enter '.', the field will be left blank.
-----
Country Name (2 letter code) [AU]:CN
State or Province Name (full name) [Some-State]:BeiJing
Locality Name (eg, city) []:BeiJing
Organization Name (eg, company) [Internet Widgits Pty Ltd]:Cernet
Organizational Unit Name (eg, section) []:NIC
Common Name (e.g. server FQDN or YOUR name) []:GongWanying    
Email Address []:gonghewan@gmail.com

Please enter the following 'extra' attributes
to be sent with your certificate request
A challenge password []:20240731
An optional company name []:Cernet
#########################################################################

#Creating a self-signed certificate
openssl x509 -req -days 365 -in server.csr -signkey server.key -out server.crt
#########################################################################
Certificate request self-signature ok
subject=C = CN, ST = BeiJing, L = BeiJing, O = Cernet, OU = NIC, CN = GongWanying, emailAddress = gonghewan@gmail.com
#########################################################################

# Certification Authority
sudo mkdir /etc/ssl/CA
sudo mkdir /etc/ssl/newcerts
# Install the certificate
sudo cp server.crt /etc/ssl/certs
sudo cp server.key /etc/ssl/private
sudo sh -c "echo '01' > /etc/ssl/CA/serial"
sudo touch /etc/ssl/CA/index.txt

# sudo vim /etc/ssl/openssl.cnf
dir             = /etc/ssl              # Where everything is kept
database        = $dir/CA/index.txt     # database index file.
certificate     = $dir/certs/cacert.pem # The CA certificate
serial          = $dir/CA/serial        # The current serial number
private_key     = $dir/private/cakey.pem# The private key

# create the self-signed root certificate
openssl req -new -x509 -extensions v3_ca -keyout cakey.pem -out cacert.pem -days 3650
#########################################################################
Enter PEM pass phrase:20240731
Verifying - Enter PEM pass phrase:20240731
-----
You are about to be asked to enter information that will be incorporated
into your certificate request.
What you are about to enter is what is called a Distinguished Name or a DN.
There are quite a few fields but you can leave some blank
For some fields there will be a default value,
If you enter '.', the field will be left blank.
-----
Country Name (2 letter code) [AU]:CN
State or Province Name (full name) [Some-State]:BeiJing
Locality Name (eg, city) []:BeiJing
Organization Name (eg, company) [Internet Widgits Pty Ltd]:Cernet
Organizational Unit Name (eg, section) []:NIC
Common Name (e.g. server FQDN or YOUR name) []:GongWanying
Email Address []:gonghewan@gmail.com
#########################################################################

sudo mv cakey.pem /etc/ssl/private/
sudo mv cacert.pem /etc/ssl/certs/
sudo openssl ca -in server.csr -config /etc/ssl/openssl.cnf
# 将上述命令输出的部分写入hostname命名的文件（-----BEGIN CERTIFICATE-----）
vim test-cernet.com.crt
# For applications that can be configured to use a CA certificate, you should also copy the /etc/ssl/certs/cacert.pem file to the /etc/ssl/certs/ directory on each server.
sudo mv test-cernet.com.crt /etc/ssl/certs/
```

## POSTFIX安装

```shell
# isntall postfix
sudo apt-update
sudo apt-get install postfix

# config postfix
sudo dpkg-reconfigure postfix
########################################################################
- Internet Site
- mail.example.com
- gwy
- test-cernet.com, localhost.localdomain, localhost
- No
- 127.0.0.0/8 \[::ffff:127.0.0.0\]/104 \[::1\]/128 192.168.0.0/24
- Yes
- 0
- +
- all
########################################################################

# This will place new mail in /home/<username>/Maildir so you will need to configure your Mail Delivery Agent (MDA) to use the same path.
sudo postconf -e 'home_mailbox = Maildir/'
# Configure SMTP authentication
sudo postconf -e 'smtpd_sasl_type = dovecot'
sudo postconf -e 'smtpd_sasl_path = private/auth'
sudo postconf -e 'smtpd_sasl_local_domain ='
sudo postconf -e 'smtpd_sasl_security_options = noanonymous'
sudo postconf -e 'broken_sasl_auth_clients = yes'
sudo postconf -e 'smtpd_sasl_auth_enable = yes'
sudo postconf -e 'smtpd_recipient_restrictions = permit_sasl_authenticated,permit_mynetworks,reject_unauth_destination'

# Configure TLS
sudo postconf -e 'smtp_tls_security_level = may'
sudo postconf -e 'smtpd_tls_security_level = may'
sudo postconf -e 'smtp_tls_note_starttls_offer = yes'
sudo postconf -e 'smtpd_tls_key_file = /etc/ssl/private/server.key'
sudo postconf -e 'smtpd_tls_cert_file = /etc/ssl/certs/server.crt'
sudo postconf -e 'smtpd_tls_loglevel = 1'
sudo postconf -e 'smtpd_tls_received_header = yes'
sudo postconf -e 'myhostname = test-cernet.com'

# If you are using your own Certificate Authority to sign the certificate, enter:
sudo postconf -e 'smtpd_tls_CAfile = /etc/ssl/certs/cacert.pem'

# restart postfix
sudo systemctl restart postfix.service

# install dovecot-core
sudo apt install dovecot-core

# config
sudo vim /etc/dovecot/conf.d/10-master.conf
########################################################################
 # Postfix smtp-auth
  unix_listener /var/spool/postfix/private/auth {
    mode = 0660
    user = postfix
    group = postfix
  }
########################################################################
# config
sudo vim /etc/dovecot/conf.d/10-auth.conf
########################################################################
auth_mechanisms = plain login
########################################################################

# restart
sudo systemctl restart dovecot.service
```

#### 主要目录

- /etc/postfix：包括postfix服务的主配文件、各类脚本、查询表等
  
  - main.cf
    
    以“#”号开头的行表示注释信息，其他行表示有效设置。设置行的格式与为shell变量赋值的形式非常类似，采用“配置参数=值”的形式，其中等号两边的空格可有可无。当某个配置参数包含多个值时，使用逗号或空格进行分隔，也允许换行进行分隔，但行首至少应有一个空格。
    
    ```shell
    inet_interfaces=192.168.20.20，127.0.0.1 ——> 监听服务的IP地址，默认为all
    myhostname=mail.topsecedu.com——> 邮件服务器的主机名
    mydomain=topsecedu.com——> 邮件域
    myorigin=$mydomain——> 外发邮件时，发件人地址的邮件域
    mydestination=mydomain,mydomain,myhostname——> 允许投递到本地目标邮件域
    ome_mailbox=Maildir/——> 设置存储位置和格式
    ```
    
    ```shell
    # See /usr/share/postfix/main.cf.dist for a commented, more complete
    # version
    
    smtpd_banner = $myhostname ESMTP $mail_name (Ubuntu)
    biff = no
    
    # appending .domain is the MUA's job.
    append_dot_mydomain = no
    
    # Uncomment the next line to generate "delayed mail" warnings
    #delay_warning_time = 4h
    # 当user收到mail时会自动创建/home/user/Maildir
    # 如果使用mailbox模式注释home_mailbox一行即可，默认会推送到/var/spool/mail/user
    home_mailbox = Maildir/
    myhostname = test-cernet.com
    alias_maps = hash:/etc/aliases
    alias_database = hash:/etc/aliases
    myorigin = /etc/mailname
    mydestination = test-cernet.com, localhost.example.com, localhost
    relayhost =
    mynetworks = 127.0.0.0/8 [::ffff:127.0.0.0]/104 [::1]/128 192.168.0.0/24
    # maildir model
    mailbox_command = 
    # maibox model
    # mailbox_command = procmail -a "$EXTENSION"
    mailbox_size_limit = 0
    recipient_delimiter = -
    inet_interfaces = all
    smtpd_sasl_local_domain =
    smtpd_sasl_auth_enable = yes
    smtpd_sasl_security_options = noanonymous
    broken_sasl_auth_clients = yes
    smtpd_recipient_restrictions = permit_sasl_authenticated,permit_mynetworks,reject_unauth_destination
    smtpd_tls_auth_only = no
    smtp_tls_security_level = may
    smtpd_tls_security_level = may
    smtp_tls_note_starttls_offer = yes
    smtpd_tls_key_file = /etc/ssl/private/server.key
    smtpd_tls_cert_file = /etc/ssl/certs/server.crt
    smtpd_tls_CAfile = /etc/ssl/certs/cacert.pem
    smtpd_tls_loglevel = 1
    smtpd_tls_received_header = yes
    smtpd_tls_session_cache_timeout = 3600s
    tls_random_source = dev:/dev/urandom
    ```

- /usr/libexec/postfix/:包括postfix服务的各个服务器程序文件

- /var/spool/postfix/:包括postfix服务的邮件队列相关的子目录，每个队列子目录用于保存不同的邮件。常见的几个子目录及用途如下所述：
  
  - incoming（传入）：存放刚接收到的邮件。
  
  - active（活动）：存放正在投递的邮件。
  
  - deferred（推迟）：存放以前投递失败的邮件。
  
  - hold（约束）：存放被阻止发送的邮件。
  
  - corrupt（错误）：存放不可读或不可分析的邮件。

- /var/log/mail.log：日志文件，同级目录下还有mail.err、mail.wrn

#### 启动失败

```shell
systemctl status postfix*
sudo tail -f /var/log/mail.err 
//没找到原因，改了改main.cf重启了一下
```

#### 测试

```shell
$ MAIL=/home/dbase/Maildir
$ mail
"/home/dbase/Maildir": 1 messge 1 new
>N   1 root               Thu Sep 19 13:45  13/461   test 20240919
? exit
you have mail
$ ls Maildir
cur  new  tmp
$ ls Maildir/new
$ ls Maildir/cur
'1726753507.Vfc01Iec0261M800668.test-cernet.com,u=1:2,'
$ vim Maildir/cur/1726753507.Vfc01Iec0261M800668.test-cernet.com,u=1:2,
```

## dovecot 安装

参考[Install and configure Dovecot - Ubuntu Server documentation](https://documentation.ubuntu.com/server/how-to/mail-services/install-dovecot/)

```shell
# install dovecot
sudo apt-update
sudo apt-get install -y dovecot-imapd dovecot-pop3d
# config dovecot
sudo vim /etc/dovecot/dovecot.conf
########################################################################
!include_try /usr/share/dovecot/protocols.d/*.protocol
########################################################################
sudo service dovecot restart

# config ssl
sudo openssl req -new -x509 -days 1000 -nodes -out "/etc/dovecot/dovecot.pem" \
    -keyout "/etc/dovecot/private/dovecot.key"
sudo vim /etc/dovecot/conf.d/10-ssl.conf
########################################################################
ssl_cert = </etc/dovecot/dovecot.pem
ssl_key = </etc/dovecot/private/dovecot.key
########################################################################
```

## maildrop安装

```shell
# libunistring2
# 下载并安装libunistring2_1.0-2_amd64.deb 和 libunistring-dev
wget https://ftp.debian.org/debian/pool/main/libu/libunistring/xxx
sudo dpkg -i ./libunistring2_1.0-2_amd64.deb

# libidn2
wget http://mirrors.kernel.org/ubuntu/pool/main/libi/libidn2/libidn2-0_2.2.0-2_amd64.deb
wget http://mirrors.kernel.org/ubuntu/pool/main/libi/libidn2/libidn2-0-dev_2.2.0-2_amd64.deb 
wget http://archive.ubuntu.com/ubuntu/pool/main/libi/libidn2/libidn2-dev_2.2.0-2_amd64.deb
sudo dpkg -i libidn2-dev_2.2.0-2_amd64.deb
sudo apt-mark hold libidn2-dev
sudo dpkg -i libidn2-0-dev_2.2.0-2_amd64.deb 
sudo apt-mark hold libidn2-0-dev
sudo dpkg -i libidn2-0_2.2.0-2_amd64.deb
sudo apt-mark hold libidn2-0

# courier-unicode
wget https://sourceforge.net/projects/courier/files/courier-unicode/2.1/courier-unicode-2.1.tar.bz2/download
mv download courier-unicode-2.1.tar.bz2
tar xivf courier-unicode-2.1.tar.bz2
cd courier-unicode-2.1
./configure
make
make install

# libcourier-unicode4
sudo apt-get install libcourier-unicode4

# pcre
sudo apt-get install g++ libpcre3-dev

# maildrop
# 先创建所需的用户信息
groupadd maildrop
sudo groupadd maildrop
sudo useradd -g maildrop maildrop
id maildrop #uid=1004(maildrop) gid=1004(maildrop) groups=1004(maildrop)

# install maildrop
wget https://sourceforge.net/projects/courier/files/maildrop/3.1.8/maildrop-3.1.8.tar.bz2/download
tar xvfj maildrop-3.1.8.tar.bz2
cd maildrop-3.1.8/
./configure --enable-maildrop-uid=1004 --enable-maildrop-gid=1004 --enable-trusted-users='root qmaild maildrop dbase gwy'
make
sudo make install
maildrop -v
chmod 4755 /usr/local/bin/maildrop
```

```shell
# check lib info
apt-cache policy libname
# check mail service 
echo "This is a test message" | mail -s 'test email' dbase@test-cernet.com
```

```shell
# mysql root拒绝登陆
FLUSH PRIVILEGES;
ALTER USER root@localhost IDENTIFIED VIA mysql_native_password 
USING PASSWORD('my secret password');
FLUSH PRIVILEGES;
```

## 实验测试

### 目前邮件存放位置

/var/spool/mail/

### 相关配置文件

- /etc/maildroprc 

```shell
SHELL="/bin/bash"

# The default path
DEFAULT="$HOME/Maildir"
MAILBOX="/var/spool/mail"
# Our log file
logfile "$HOME/maildrop.log"

# Our verbosity in the log file
VERBOSE="5"

# This get's added above each entry in the log file.
# It's just a visual aid.
log "-----------A NEW MAIL IN-------------"

# If the Spam-Flag is YES, sort to Junk folder
# if ( /^X-Spam-Flag: YES/)
#{
#to $DEFAULT/.SPAM/
#}

if ( /^Subject:.*HOWTO/)
{
`/home/dbase/scripts/process.sh`
to $MAILBOX/dbase
log "------SEND HOWTO MAIL INTO DBASE BOX------"
}
if ( /^Subject:.*HELP/)
{
`/home/dbase/scripts/process.sh`
to $MAILBOX/dbase
log "------SEND HELP MAIL INTO DBASE BOX------"
}
if ( /^Subject:.*NEW/)
{
`/home/dbase/scripts/process.sh`
to $MAILBOX/dbase
log "-------SEND NEW MAIL INTO DBASE BOX------"
}
to $MAILBOX/else
```

- /etc/postfix/main.cf

```shell
# See /usr/share/postfix/main.cf.dist for a commented, more complete
# version

smtpd_banner = $myhostname ESMTP $mail_name (Ubuntu)
biff = no

# appending .domain is the MUA's job.
append_dot_mydomain = no

# Uncomment the next line to generate "delayed mail" warnings
#delay_warning_time = 4h
#home_mailbox = Maildir/ 
myhostname = test-cernet.com
alias_maps = hash:/etc/aliases
alias_database = hash:/etc/aliases
myorigin = /etc/mailname
mydestination = test-cernet.com, localhost.example.com, localhost
relayhost =
mynetworks = 127.0.0.0/8 [::ffff:127.0.0.0]/104 [::1]/128 192.168.0.0/24
mailbox_command = /usr/local/bin/maildrop -d ${USER}
# procmail -a "$EXTENSION"
mailbox_size_limit = 0
recipient_delimiter = -
inet_interfaces = all
smtpd_sasl_local_domain =
smtpd_sasl_auth_enable = yes
smtpd_sasl_security_options = noanonymous
broken_sasl_auth_clients = yes
smtpd_recipient_restrictions = permit_sasl_authenticated,permit_mynetworks,reject_unauth_destination
smtpd_tls_auth_only = no
smtp_tls_security_level = may
smtpd_tls_security_level = may
smtp_tls_note_starttls_offer = yes
smtpd_tls_key_file = /etc/ssl/private/server.key
smtpd_tls_cert_file = /etc/ssl/certs/server.crt
smtpd_tls_CAfile = /etc/ssl/certs/cacert.pem
smtpd_tls_loglevel = 1
smtpd_tls_received_header = yes
smtpd_tls_session_cache_timeout = 3600s
tls_random_source = dev:/dev/urandom
virtual_transport = maildrop
virtual_uid_maps = static:1004
virtual_gid_maps = static:1004
```

- /etc/postfix/master.cf
  
  ```shell
  maildrop  unix  -       n       n       -       -       pipe
  flags=DRXhu user=maildrop argv=/usr/local/bin/maildrop -d ${recipient}
  ```

- /home/dbase/scripts/process.sh 

```shell
echo "process start" >> /home/dbase/maildrop.log
IN=$(cat /dev/stdin)
# write file
# echo "${IN}" > /home/dbase/test.txt

#echo "process end" >> /home/dbase/maildrop.log
my_command='mysql -hlocalhost -uroot -p"20240731" -s MAILUPDATES_LOCAL -e "INSERT INTO mailupdates (message, changed) VALUES ('"'"$IN"'"', UNIX_TIMESTAMP());"'
echo $my_command >> /home/dbase/maildrop.log
eval $my_command
mysql -hlocalhost -uroot -p'20240731' -s MAILUPDATES_LOCAL -e "select * from  mailupdates;" >> /home/dbase/maildrop.log
echo "process done" >> /home/dbase/maildrop.log
```
