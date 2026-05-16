create table if not exists edi_partner_warehouses (
    partner_id bigint not null references edi_partners(id),
    warehouse_id bigint not null references warehouses(id),
    primary key (partner_id, warehouse_id)
);

insert into edi_partner_warehouses (partner_id, warehouse_id)
select id, default_warehouse_id
from edi_partners
where default_warehouse_id is not null
on conflict do nothing;

alter table edi_partners drop column if exists default_warehouse_id;
