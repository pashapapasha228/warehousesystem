create table if not exists product_warehouse_min_stock (
    id bigserial primary key,
    product_id bigint not null references products(id),
    warehouse_id bigint not null references warehouses(id),
    min_stock_level integer not null default 0,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    constraint uk_product_warehouse_min_stock unique (product_id, warehouse_id),
    constraint ck_product_warehouse_min_stock_non_negative check (min_stock_level >= 0)
);

insert into product_warehouse_min_stock (product_id, warehouse_id, min_stock_level, created_at, updated_at)
select p.id, w.id, greatest(coalesce(p.min_stock_level, 0), 0), now(), now()
from products p
cross join warehouses w
where exists (
    select 1
    from information_schema.columns c
    where c.table_schema = 'public'
      and c.table_name = 'products'
      and c.column_name = 'min_stock_level'
)
on conflict (product_id, warehouse_id) do nothing;

update products set category = case category
    when 'Электроника' then 'ELECTRONICS'
    when 'Комплектующие' then 'COMPONENTS'
    when 'Кабельная продукция' then 'CABLES'
    when 'Инструменты' then 'TOOLS'
    when 'Офис' then 'OFFICE'
    when 'Расходники' then 'CONSUMABLES'
    when 'Сетевое оборудование' then 'NETWORK'
    when 'Серверное оборудование' then 'SERVER'
    when 'ELECTRONICS' then 'ELECTRONICS'
    when 'COMPONENTS' then 'COMPONENTS'
    when 'CABLES' then 'CABLES'
    when 'TOOLS' then 'TOOLS'
    when 'OFFICE' then 'OFFICE'
    when 'CONSUMABLES' then 'CONSUMABLES'
    when 'NETWORK' then 'NETWORK'
    when 'SERVER' then 'SERVER'
    when 'OTHER' then 'OTHER'
    else 'OTHER'
end;

update products_aud set category = case category
    when 'Электроника' then 'ELECTRONICS'
    when 'Комплектующие' then 'COMPONENTS'
    when 'Кабельная продукция' then 'CABLES'
    when 'Инструменты' then 'TOOLS'
    when 'Офис' then 'OFFICE'
    when 'Расходники' then 'CONSUMABLES'
    when 'Сетевое оборудование' then 'NETWORK'
    when 'Серверное оборудование' then 'SERVER'
    when 'ELECTRONICS' then 'ELECTRONICS'
    when 'COMPONENTS' then 'COMPONENTS'
    when 'CABLES' then 'CABLES'
    when 'TOOLS' then 'TOOLS'
    when 'OFFICE' then 'OFFICE'
    when 'CONSUMABLES' then 'CONSUMABLES'
    when 'NETWORK' then 'NETWORK'
    when 'SERVER' then 'SERVER'
    when 'OTHER' then 'OTHER'
    else 'OTHER'
end
where category is not null;

alter table products alter column category set default 'OTHER';
update products set category = 'OTHER' where category is null;
alter table products alter column category set not null;

do $$
begin
    if not exists (
        select 1 from pg_constraint where conname = 'ck_products_category'
    ) then
        alter table products add constraint ck_products_category
            check (category in ('ELECTRONICS', 'COMPONENTS', 'CABLES', 'TOOLS', 'OFFICE', 'CONSUMABLES', 'NETWORK', 'SERVER', 'OTHER'));
    end if;
end $$;

alter table products drop column if exists min_stock_level;
alter table products_aud drop column if exists min_stock_level;
