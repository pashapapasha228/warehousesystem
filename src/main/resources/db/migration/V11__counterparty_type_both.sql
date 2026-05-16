alter table counterparties drop constraint if exists ck_counterparties_type;

alter table counterparties add constraint ck_counterparties_type
    check (type in ('SUPPLIER', 'CUSTOMER', 'BOTH'));
