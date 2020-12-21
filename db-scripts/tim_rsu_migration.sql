-- Remove globally-unique index
ALTER TABLE "CVCOMMS"."TIM_RSU" DROP CONSTRAINT TIM_RSU_PK;
DROP INDEX TIM_RSU_PK;
COMMIT;

-- Create new TIM_RSU table
CREATE TABLE "CVCOMMS"."TIM_RSU_NEW" 
(	"TIM_RSU_ID" NUMBER(10,0) NOT NULL, 
    "RSU_ID" NUMBER(10,0) NOT NULL REFERENCES "CVCOMMS"."RSU" ("RSU_ID") ON DELETE CASCADE ENABLE, 
    "TIM_ID" NUMBER(10,0) NOT NULL REFERENCES "CVCOMMS"."TIM" ("TIM_ID") ON DELETE CASCADE ENABLE, 
    "RSU_INDEX" NUMBER(10,0)
)   NO INMEMORY ;

-- Rename old trigger, add trigger on new table
ALTER TRIGGER "CVCOMMS"."TRG_TIM_RSU_ID" RENAME TO TRG_TIM_RSU_OLD_ID;

CREATE OR REPLACE EDITIONABLE TRIGGER "CVCOMMS"."TRG_TIM_RSU_ID" 
before insert on TIM_RSU_NEW
for each row
begin
select tim_rsu_id_seq.nextval
into :new.tim_rsu_id
from dual;
end;

ALTER TRIGGER "CVCOMMS"."TRG_TIM_RSU_ID" ENABLE;

-- PK index
CREATE UNIQUE INDEX "CVCOMMS"."TIM_RSU_PK" ON "CVCOMMS"."TIM_RSU_NEW" ("TIM_RSU_ID");

-- Constraints
ALTER TABLE "CVCOMMS"."TIM_RSU_NEW" ADD CONSTRAINT "TIM_RSU_U" UNIQUE (rsu_id, tim_id, rsu_index)
USING INDEX  ENABLE;
ALTER TABLE "CVCOMMS"."TIM_RSU_NEW" ADD CONSTRAINT "TIM_RSU_PK" PRIMARY KEY ("TIM_RSU_ID")
USING INDEX  ENABLE;

COMMIT;

-- Insert old data
INSERT INTO TIM_RSU_NEW(rsu_id, tim_id, rsu_index)
SELECT DISTINCT rsu_id, tim_id, rsu_index FROM TIM_RSU;

COMMIT;

ALTER TABLE TIM_RSU RENAME TO TIM_RSU_OLD;
ALTER TABLE TIM_RSU_NEW RENAME TO TIM_RSU;
COMMIT;


-- DROP TRIGGER "CVCOMMS"."TRG_TIM_RSU_OLD_ID";
-- DROP TABLE "CVCOMMS"."TIM_RSU_OLD";
