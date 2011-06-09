-- 系统标识相关模块

-- 参与者的扩展属性
create table BC_IDENTITY_ACTOR_DETAIL (
    ID number(19) NOT NULL,
    CREATEDATE date,
    FIRST_NAME varchar(45),
    LAST_NAME varchar(45),
    SEX number(1),
    primary key (ID)
);
COMMENT ON TABLE BC_IDENTITY_ACTOR_DETAIL IS '参与者的扩展属性';
COMMENT ON COLUMN BC_IDENTITY_ACTOR_DETAIL.CREATEDATE IS '创建时间';
COMMENT ON COLUMN BC_IDENTITY_ACTOR_DETAIL.FIRST_NAME IS 'user-姓氏';
COMMENT ON COLUMN BC_IDENTITY_ACTOR_DETAIL.LAST_NAME IS 'user-名字';
COMMENT ON COLUMN BC_IDENTITY_ACTOR_DETAIL.SEX IS 'user-性别：1-男,2-女';

-- 参与者
create table BC_IDENTITY_ACTOR (
    ID number(19) NOT NULL,
    UID_ varchar(36) NOT NULL,
    TYPE_ number(1) NOT NULL,
    STATUS_ number(1) NOT NULL,
    INNER_ number(1) NOT NULL,
    CODE varchar(100) NOT NULL,
    NAME varchar(255) NOT NULL,
    ORDER_ varchar(100),
    EMAIL varchar(255),
    PHONE varchar(255),
    DETAIL_ID number(19),
    primary key (ID)
);
ALTER TABLE BC_IDENTITY_ACTOR ADD CONSTRAINT FK_ACTOR_ACTORDETAIL FOREIGN KEY (DETAIL_ID) 
	REFERENCES BC_IDENTITY_ACTOR_DETAIL (ID) ON DELETE CASCADE;
CREATE INDEX IDX_ACTOR_TYPE ON BC_IDENTITY_ACTOR (TYPE_ ASC);
COMMENT ON TABLE BC_IDENTITY_ACTOR IS '参与者(代表一个人或组织，组织也可以是单位、部门、岗位、团队等)';
COMMENT ON COLUMN BC_IDENTITY_ACTOR.UID_ IS '全局标识';
COMMENT ON COLUMN BC_IDENTITY_ACTOR.TYPE_ IS '类型：1-文件夹,2-内部链接,3-外部链接,4-html';
COMMENT ON COLUMN BC_IDENTITY_ACTOR.STATUS_ IS '状态：0-已禁用,1-启用中,2-已删除';
COMMENT ON COLUMN BC_IDENTITY_ACTOR.INNER_ IS '是否为内置对象:0-否,1-是';
COMMENT ON COLUMN BC_IDENTITY_ACTOR.CODE IS '编码';
COMMENT ON COLUMN BC_IDENTITY_ACTOR.NAME IS '名称';
COMMENT ON COLUMN BC_IDENTITY_ACTOR.ORDER_ IS '同类参与者之间的排序号';
COMMENT ON COLUMN BC_IDENTITY_ACTOR.EMAIL IS '邮箱';
COMMENT ON COLUMN BC_IDENTITY_ACTOR.PHONE IS '联系电话';
COMMENT ON COLUMN BC_IDENTITY_ACTOR.DETAIL_ID IS '扩展表的ID';

-- 参与者之间的关联关系
create table BC_IDENTITY_ACTOR_RELATION (
    TYPE_ number(2) NOT NULL,
    MASTER_ID int NOT NULL,
   	FOLLOWER_ID int NOT NULL,
    ORDER_ varchar(100),
    primary key (MASTER_ID,FOLLOWER_ID,TYPE_)
);
ALTER TABLE BC_IDENTITY_ACTOR_RELATION ADD CONSTRAINT FK_AR_MASTER FOREIGN KEY (MASTER_ID) 
	REFERENCES BC_IDENTITY_ACTOR (ID);
ALTER TABLE BC_IDENTITY_ACTOR_RELATION ADD CONSTRAINT FK_AR_FOLLOWER FOREIGN KEY (FOLLOWER_ID) 
	REFERENCES BC_IDENTITY_ACTOR (ID);
CREATE INDEX IDX_AR_TM ON BC_IDENTITY_ACTOR_RELATION (TYPE_, MASTER_ID);
CREATE INDEX IDX_AR_TF ON BC_IDENTITY_ACTOR_RELATION (TYPE_, FOLLOWER_ID);
COMMENT ON TABLE BC_IDENTITY_ACTOR_RELATION IS '参与者之间的关联关系';
COMMENT ON COLUMN BC_IDENTITY_ACTOR_RELATION.TYPE_ IS '关联类型';
COMMENT ON COLUMN BC_IDENTITY_ACTOR_RELATION.MASTER_ID IS '主控方ID';
COMMENT ON COLUMN BC_IDENTITY_ACTOR_RELATION.FOLLOWER_ID IS '从属方ID';
COMMENT ON COLUMN BC_IDENTITY_ACTOR_RELATION.ORDER_ IS '从属方之间的排序号';

-- 职务
create table BC_IDENTITY_DUTY (
    ID int NOT NULL,
    UID_ varchar(36),
    STATUS_ number(1) NOT NULL,
    INNER_ number(1) NOT NULL,
    CODE varchar(100) NOT NULL,
    NAME varchar(255) NOT NULL,
    primary key (ID)
);
COMMENT ON TABLE BC_IDENTITY_DUTY IS '职务';
COMMENT ON COLUMN BC_IDENTITY_DUTY.UID_ IS '全局标识';
COMMENT ON COLUMN BC_IDENTITY_DUTY.STATUS_ IS '状态：0-已禁用,1-启用中,2-已删除';
COMMENT ON COLUMN BC_IDENTITY_DUTY.INNER_ IS '是否为内置对象:0-否,1-是';
COMMENT ON COLUMN BC_IDENTITY_DUTY.CODE IS '编码';
COMMENT ON COLUMN BC_IDENTITY_DUTY.NAME IS '名称';

-- 标识生成器
CREATE TABLE BC_IDENTITY_IDGENERATOR (
  TYPE_ VARCHAR(45) NOT NULL,
  VALUE INT NOT NULL,
  FORMAT VARCHAR(45) ,
  PRIMARY KEY (TYPE_)
);
COMMENT ON TABLE BC_IDENTITY_IDGENERATOR IS '标识生成器,用于生成主键或唯一编码用';
COMMENT ON COLUMN BC_IDENTITY_IDGENERATOR.TYPE_ IS '分类';
COMMENT ON COLUMN BC_IDENTITY_IDGENERATOR.VALUE IS '当前值';
COMMENT ON COLUMN BC_IDENTITY_IDGENERATOR.FORMAT IS '格式模板,如“case-${V}”、“${T}-${V}”,V代表value的值，T代表type_的值';

-- 参与者与角色的关联
create table BC_SECURITY_ROLE_ACTOR (
    AID number(19) NOT NULL,
    RID number(19) NOT NULL,
    primary key (AID,RID)
);
ALTER TABLE BC_SECURITY_ROLE_ACTOR ADD CONSTRAINT FK_RA_ACTOR FOREIGN KEY (AID) 
	REFERENCES BC_IDENTITY_ACTOR (ID);
ALTER TABLE BC_SECURITY_ROLE_ACTOR ADD CONSTRAINT FK_RA_ROLE FOREIGN KEY (RID) 
	REFERENCES BC_SECURITY_ROLE (ID);
COMMENT ON TABLE BC_SECURITY_ROLE_ACTOR IS '参与者与角色的关联：参与者拥有哪些角色';
COMMENT ON COLUMN BC_SECURITY_ROLE_ACTOR.AID IS '参与者ID';
COMMENT ON COLUMN BC_SECURITY_ROLE_ACTOR.RID IS '角色ID';
