alter table counterparties drop constraint if exists counterparties_type_check;
alter table counterparties drop constraint if exists ck_counterparties_type;

alter table counterparties add constraint ck_counterparties_type
    check (type in ('SUPPLIER', 'CUSTOMER', 'BOTH'));

update counterparties
set type = 'BOTH'
where code = 'CUS-REGIONPLUS';
