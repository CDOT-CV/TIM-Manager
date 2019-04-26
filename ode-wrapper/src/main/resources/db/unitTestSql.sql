CREATE TABLE IF NOT EXISTS TIM 
(	
    TIM_ID NUMBER(10,0) NOT NULL AUTO_INCREMENT, 
    MSG_CNT VARCHAR2(255), 
    URL_B VARCHAR2(255), 
    TIME_STAMP TIMESTAMP (6), 
    RECORD_GENERATED_BY VARCHAR2(5), 
    RMD_LD_ELEVATION NUMBER(12,6), 
    RMD_LD_HEADING NUMBER(12,8), 
    RMD_LD_LATITUDE NUMBER(12,8), 
    RMD_LD_LONGITUDE NUMBER(12,8), 
    RMD_LD_SPEED NUMBER(12,6), 
    RMD_RX_SOURCE VARCHAR2(5), 
    SCHEMA_VERSION NUMBER(10,0), 
    VALID_SIGNATURE NUMBER(1,0), 
    LOG_FILE_NAME VARCHAR2(255), 
    RECORD_GENERATED_AT TIMESTAMP (6), 
    SANITIZED NUMBER(1,0), 
    SERIAL_ID_STREAM_ID VARCHAR2(255), 
    SERIAL_ID_BUNDLE_SIZE NUMBER(10,0), 
    SERIAL_ID_BUNDLE_ID NUMBER(10,0), 
    SERIAL_ID_RECORD_ID NUMBER(10,0), 
    SERIAL_ID_SERIAL_NUMBER NUMBER(10,0), 
    PAYLOAD_TYPE VARCHAR2(255), 
    RECORD_TYPE VARCHAR2(255), 
    ODE_RECEIVED_AT TIMESTAMP (6), 
    PACKET_ID VARCHAR2(50), 
    PRIMARY KEY (TIM_ID),
    SECURITY_RESULT_CODE NUMBER(10,0),
    SAT_RECORD_ID VARCHAR2(10),
    TIM_NAME VARCHAR2(100)
);

CREATE TABLE IF NOT EXISTS SECURITY_RESULT_CODE_TYPE
( 
    SECURITY_RESULT_CODE_TYPE_ID NUMBER(10,0) NOT NULL AUTO_INCREMENT, 
    SECURITY_RESULT_CODE_TYPE VARCHAR2(255), 
    PRIMARY KEY (SECURITY_RESULT_CODE_TYPE_ID)
);

CREATE TABLE IF NOT EXISTS CATEGORY
( 
    CATEGORY_ID NUMBER(10,0) NOT NULL AUTO_INCREMENT, 
    CATEGORY VARCHAR2(255), 
    PRIMARY KEY (CATEGORY_ID)
);

CREATE TABLE IF NOT EXISTS ITIS_CODE
( 
    ITIS_CODE_ID NUMBER(10,0) NOT NULL AUTO_INCREMENT, 
    DESCRIPTION VARCHAR2(255) NOT NULL, 
    CATEGORY_ID NUMBER(10,0) NOT NULL, 
    ITIS_CODE NUMBER(10,0),       
    PRIMARY KEY (ITIS_CODE_ID),
    FOREIGN KEY(CATEGORY_ID) REFERENCES CATEGORY(CATEGORY_ID)
);

CREATE TABLE IF NOT EXISTS ACTIVE_TIM
( 
    ACTIVE_TIM_ID NUMBER(10,0) NOT NULL AUTO_INCREMENT,
    TIM_ID NUMBER(10,0), 
    MILEPOST_START NUMBER(8,3), 
    MILEPOST_STOP NUMBER(8,3), 
    DIRECTION VARCHAR2(50), 
    TIM_START TIMESTAMP (6), 
    TIM_END TIMESTAMP (6), 
    TIM_TYPE_ID NUMBER(10,0), 
    CLIENT_ID VARCHAR2(255),
    ROUTE VARCHAR2(255),
    SAT_RECORD_ID VARCHAR2(8),
    PK NUMBER(10, 0),
    PRIMARY KEY (ACTIVE_TIM_ID),
    FOREIGN KEY(TIM_ID) REFERENCES TIM(TIM_ID) ON DELETE CASCADE   
);

CREATE TABLE IF NOT EXISTS ACTIVE_TIM_ITIS_CODE
( 
    ACTIVE_TIM_ITIS_CODE_ID NUMBER(10,0) NOT NULL AUTO_INCREMENT,
    ACTIVE_TIM_ID NUMBER(10,0) NOT NULL, 
    ITIS_CODE_ID NUMBER(10,0) NOT NULL, 
    PRIMARY KEY (ACTIVE_TIM_ITIS_CODE_ID),
    FOREIGN KEY(ACTIVE_TIM_ID) REFERENCES ACTIVE_TIM(ACTIVE_TIM_ID) ON DELETE CASCADE,  
    FOREIGN KEY(ITIS_CODE_ID) REFERENCES ITIS_CODE(ITIS_CODE_ID)  
);

CREATE TABLE IF NOT EXISTS RSU
( 
    RSU_ID NUMBER(10,0) NOT NULL AUTO_INCREMENT,
    DEVICEID NUMBER(6,0), 
    PRIMARY KEY (RSU_ID)
);

CREATE TABLE IF NOT EXISTS TIM_RSU
( 
    TIM_RSU_ID NUMBER(10,0) NOT NULL AUTO_INCREMENT, 
    RSU_ID NUMBER(10,0) NOT NULL, 
    TIM_ID NUMBER(10,0) NOT NULL, 
    RSU_INDEX NUMBER(10,0),
    PRIMARY KEY (TIM_RSU_ID),
    FOREIGN KEY(RSU_ID) REFERENCES RSU(RSU_ID),
    FOREIGN KEY(TIM_ID) REFERENCES TIM(TIM_ID) ON DELETE CASCADE         
);


CREATE TABLE IF NOT EXISTS MILEPOST_VW
(
    ROUTE VARCHAR2(255),
    MILEPOST NUMBER(38, 8),
    DIRECTION VARCHAR2(255),
    LATITUDE NUMBER(38, 8),
    LONGITUDE NUMBER(38, 8),
    ELEVATION_FT NUMBER(38, 8),
    BEARING NUMBER(38, 8)
);

CREATE TABLE IF NOT EXISTS RSU_VW
(
    DEVICEID NUMBER(6),
    SITENAME VARCHAR2(750),
    DEVICENAME VARCHAR2(300),
    DEVICETYPE VARCHAR2(150),
    MANUFACTNAME VARCHAR2(150),
    MODELDESCRIPTION VARCHAR2(750),
    MODELNUMBER VARCHAR2(150),
    STATUS VARCHAR2(36),
    LATITUDE NUMBER(15, 8),
    LONGITUDE NUMBER(15, 8),
    CATEGORY VARCHAR2(6),
    IDNUMBER NUMBER(10),
    DIRECTION VARCHAR2(3),
    ROUTE VARCHAR2(12000),
    MILEPOST NUMBER(6, 3),
    POWERTYPE VARCHAR2(300),
    COMMDESC VARCHAR2(150),
    DISTRICT NUMBER(1),
    IPV4_ADDRESS VARCHAR2(489),
    IPV6_ADDRESS VARCHAR2(1104)
);

CREATE TABLE IF NOT EXISTS TIM_TYPE
(	
    TYPE VARCHAR2(10), 
    DESCRIPTION VARCHAR2(255), 
    TIM_TYPE_ID NUMBER(10,0) NOT NULL AUTO_INCREMENT,
    PRIMARY KEY (TIM_TYPE_ID)
);


insert into rsu_vw (DEVICEID, STATUS, MILEPOST, IPV4_ADDRESS, ROUTE) values (4499, 'Existing', 317.2, '0.0.0.0', 'I80');
insert into rsu_vw (DEVICEID, STATUS, MILEPOST, IPV4_ADDRESS, ROUTE) values (4497, 'Existing', 322.6, '0.0.0.0', 'I80');
insert into rsu_vw (DEVICEID, STATUS, MILEPOST, IPV4_ADDRESS, ROUTE) values (4495, 'Existing', 323.05, '0.0.0.0', 'I80');
insert into rsu_vw (DEVICEID, STATUS, MILEPOST, IPV4_ADDRESS, ROUTE) values (4494, 'Existing', 324.9, '0.0.0.0', 'I80');
insert into rsu_vw (DEVICEID, STATUS, MILEPOST, IPV4_ADDRESS, ROUTE) values (4493, 'Existing', 341.6, '0.0.0.0', 'I80');
insert into rsu_vw (DEVICEID, STATUS, MILEPOST, IPV4_ADDRESS, ROUTE) values (4492, 'Existing', 343.24, '0.0.0.0', 'I80');
insert into rsu_vw (DEVICEID, STATUS, MILEPOST, IPV4_ADDRESS, ROUTE) values (4491, 'Existing', 345.56, '0.0.0.0', 'I80');
insert into rsu_vw (DEVICEID, STATUS, MILEPOST, IPV4_ADDRESS, ROUTE) values (4600, 'Existing', 369.8, '0.0.0.0', 'I80');
insert into rsu_vw (DEVICEID, STATUS, MILEPOST, IPV4_ADDRESS, ROUTE) values (4487, 'Existing', 401.8, '0.0.0.0', 'I80');

insert into rsu (DEVICEID) values (4499);
insert into rsu (DEVICEID) values (4497);
insert into rsu (DEVICEID) values (4495);
insert into rsu (DEVICEID) values (4494);
insert into rsu (DEVICEID) values (4493);
insert into rsu (DEVICEID) values (4492);
insert into rsu (DEVICEID) values (4491);
insert into rsu (DEVICEID) values (4600);
insert into rsu (DEVICEID) values (4487);

insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 340, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 341, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 342, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 343, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 344, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 345, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 346, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 347, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 348, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 349, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 350, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 351, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 352, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 353, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 354, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 355, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 356, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 357, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 358, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 359, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 360, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 361, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 362, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 363, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 364, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 365, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 366, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 367, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 368, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 369, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 370, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 371, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 372, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 373, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 374, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 375, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 376, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 377, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 378, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 379, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 380, 'westbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);

insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 340, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 341, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 342, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 343, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 344, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 345, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 346, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 347, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 348, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 349, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 350, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 351, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 352, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 353, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 354, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 355, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 356, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 357, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 358, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 359, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 360, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 361, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 362, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 363, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 364, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 365, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 366, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 367, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 368, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 369, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 370, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 371, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 372, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 373, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 374, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 375, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 376, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 377, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 378, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 379, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);
insert into milepost_vw (ROUTE, MILEPOST, DIRECTION, LATITUDE, LONGITUDE, ELEVATION_FT, BEARING) values ('I 80', 380, 'eastbound', 41.12438849, -104.75521179, 5973.51133383, 268.81053377);

insert into category (category) values ('speedLimit');
insert into category (category) values ('advisory');
insert into category (category) values ('workZone');
insert into category (category) values ('exitService');

insert into itis_code (description, category_id, itis_code) values ('Speed Limit', 1, 268);
insert into itis_code (description, category_id, itis_code) values ('45', 1, 12589);
insert into itis_code (description, category_id, itis_code) values ('40', 1, 12584);
insert into itis_code (description, category_id, itis_code) values ('mph', 1, 8720);
insert into itis_code (description, category_id, itis_code) values ('Winter Storm', 1, 4871);
insert into itis_code (description, category_id, itis_code) values ('Fog', 1, 5378);
insert into itis_code (description, category_id, itis_code) values ('Mudslide', 1, 1307);
insert into itis_code (description, category_id, itis_code) values ('Fire', 1, 3200);
insert into itis_code (description, category_id, itis_code) values ('Spaces available', 1, 4105);
insert into itis_code (description, category_id, itis_code) values ('No parking spaces available', 1, 4103);
insert into itis_code (description, category_id, itis_code) values ('Reduced to one lane', 1, 777);
insert into itis_code (description, category_id, itis_code) values ('left', 1, 13580);
insert into itis_code (description, category_id, itis_code) values ('workers', 1, 224);
insert into itis_code (description, category_id, itis_code) values ('Road Construction', 1, 1025);

insert into tim_type (type, description) values ('VSL', 'Varaible Speed Limit');
insert into tim_type (type, description) values ('RC', 'Road Condition');
insert into tim_type (type, description) values ('RW', 'Road Construction');
insert into tim_type (type, description) values ('I', 'Incident');
insert into tim_type (type, description) values ('CC', 'Chain Controls');
insert into tim_type (type, description) values ('P', 'Parking');

CREATE TABLE IF NOT EXISTS INCIDENT_PROBLEM_LUT
(	
    CODE VARCHAR2(10), 
    ITIS_CODE_ID NUMBER(10,0),
    DESCRIPTION VARCHAR2(255), 
    INCIDENT_PROBLEM_LUT_ID NUMBER(10,0) NOT NULL AUTO_INCREMENT,
    PRIMARY KEY (INCIDENT_PROBLEM_LUT_ID)
);

insert into INCIDENT_PROBLEM_LUT (itis_code_id, code, description) values (6, 'mudslide', 'Mudslide');
insert into INCIDENT_PROBLEM_LUT (itis_code_id, code, description) values (7, 'fire', 'Fire');

CREATE TABLE IF NOT EXISTS INCIDENT_EFFECT_LUT
(	
    CODE VARCHAR2(10), 
    ITIS_CODE_ID NUMBER(10,0),
    DESCRIPTION VARCHAR2(255), 
    INCIDENT_EFFECT_LUT_ID NUMBER(10,0) NOT NULL AUTO_INCREMENT,
    PRIMARY KEY (INCIDENT_EFFECT_LUT_ID)
);

CREATE TABLE IF NOT EXISTS INCIDENT_ACTION_LUT
(	
    CODE VARCHAR2(10), 
    ITIS_CODE_ID NUMBER(10,0),
    DESCRIPTION VARCHAR2(255), 
    INCIDENT_ACTION_LUT_ID NUMBER(10,0) NOT NULL AUTO_INCREMENT,
    PRIMARY KEY (INCIDENT_ACTION_LUT_ID)
);

insert into SECURITY_RESULT_CODE_TYPE (SECURITY_RESULT_CODE_TYPE) values ('success');
insert into SECURITY_RESULT_CODE_TYPE (SECURITY_RESULT_CODE_TYPE) values ('unknown');