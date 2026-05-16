alter table edi_messages drop constraint if exists ck_edi_messages_status;
alter table edi_messages add constraint ck_edi_messages_status
    check (status in ('RECEIVED', 'NORMALIZED', 'PROCESSING', 'PROCESSED', 'COMPLETED', 'FAILED'));

alter table operations drop constraint if exists ck_operations_status;
alter table operations add constraint ck_operations_status
    check (status in ('DRAFT', 'SHIPPED', 'COMPLETED', 'CANCELLED'));
