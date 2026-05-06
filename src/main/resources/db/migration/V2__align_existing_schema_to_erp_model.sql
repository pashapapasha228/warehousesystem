alter table users add column if not exists full_name varchar(255);
alter table users add column if not exists email varchar(150);
alter table users add column if not exists is_active boolean not null default true;
alter table users add column if not exists created_at timestamp not null default current_timestamp;
alter table users add column if not exists updated_at timestamp not null default current_timestamp;
update users set role = replace(role, 'ROLE_', '') where role like 'ROLE_%';

alter table products add column if not exists barcode varchar(100);
alter table products add column if not exists min_stock_level integer not null default 0;
alter table products add column if not exists volume_per_unit_cm3 numeric(14, 3) not null default 0;
alter table products add column if not exists length_cm numeric(10, 2) not null default 0;
alter table products add column if not exists width_cm numeric(10, 2) not null default 0;
alter table products add column if not exists height_cm numeric(10, 2) not null default 0;
alter table products add column if not exists is_active boolean not null default true;
alter table products add column if not exists created_at timestamp not null default current_timestamp;
alter table products add column if not exists updated_at timestamp not null default current_timestamp;
do $$
begin
    if exists (
        select 1 from information_schema.columns
        where table_schema = 'public' and table_name = 'products' and column_name = 'volume_per_unit_cubic_cm'
    ) then
        update products set volume_per_unit_cm3 = volume_per_unit_cubic_cm where volume_per_unit_cubic_cm is not null and volume_per_unit_cm3 = 0;
    end if;

    if exists (
        select 1 from information_schema.columns
        where table_schema = 'public' and table_name = 'products' and column_name = 'length_per_unit_cm'
    ) then
        update products set length_cm = length_per_unit_cm where length_per_unit_cm is not null and length_cm = 0;
    end if;

    if exists (
        select 1 from information_schema.columns
        where table_schema = 'public' and table_name = 'products' and column_name = 'width_per_unit_cm'
    ) then
        update products set width_cm = width_per_unit_cm where width_per_unit_cm is not null and width_cm = 0;
    end if;

    if exists (
        select 1 from information_schema.columns
        where table_schema = 'public' and table_name = 'products' and column_name = 'height_per_unit_cm'
    ) then
        update products set height_cm = height_per_unit_cm where height_per_unit_cm is not null and height_cm = 0;
    end if;
end $$;
alter table products alter column unit_of_measure set default 'pcs';
update products set unit_of_measure = 'pcs' where unit_of_measure is null;
alter table products alter column unit_of_measure set not null;
update products set weight_per_unit_kg = 0 where weight_per_unit_kg is null;
alter table products alter column weight_per_unit_kg set default 0;
alter table products alter column weight_per_unit_kg set not null;

alter table warehouses add column if not exists code varchar(50);
alter table warehouses add column if not exists address varchar(255);
alter table warehouses add column if not exists is_active boolean not null default true;
alter table warehouses add column if not exists created_at timestamp not null default current_timestamp;
alter table warehouses add column if not exists updated_at timestamp not null default current_timestamp;
update warehouses set code = 'WH-' || id where code is null;
alter table warehouses alter column code set not null;

alter table storage_cells add column if not exists zone varchar(50);
alter table storage_cells add column if not exists rack varchar(50);
alter table storage_cells add column if not exists shelf varchar(50);
alter table storage_cells add column if not exists level varchar(50);
alter table storage_cells add column if not exists capacity_units integer not null default 0;
alter table storage_cells add column if not exists max_volume_cm3 numeric(14, 3) not null default 0;
alter table storage_cells add column if not exists current_volume_cm3 numeric(14, 3) not null default 0;
alter table storage_cells add column if not exists is_active boolean not null default true;
alter table storage_cells add column if not exists created_at timestamp not null default current_timestamp;
alter table storage_cells add column if not exists updated_at timestamp not null default current_timestamp;
do $$
begin
    if exists (
        select 1 from information_schema.columns
        where table_schema = 'public' and table_name = 'storage_cells' and column_name = 'capacity'
    ) then
        update storage_cells set capacity_units = capacity where capacity is not null and capacity_units = 0;
    end if;

    if exists (
        select 1 from information_schema.columns
        where table_schema = 'public' and table_name = 'storage_cells' and column_name = 'current_volume_cubic_cm'
    ) then
        update storage_cells set current_volume_cm3 = current_volume_cubic_cm where current_volume_cubic_cm is not null and current_volume_cm3 = 0;
    end if;
end $$;
update storage_cells set max_volume_cm3 = length_cm * width_cm * height_cm where max_volume_cm3 = 0;
update storage_cells set max_weight_kg = 0 where max_weight_kg is null;
update storage_cells set length_cm = 0 where length_cm is null;
update storage_cells set width_cm = 0 where width_cm is null;
update storage_cells set height_cm = 0 where height_cm is null;
update storage_cells set current_weight_kg = 0 where current_weight_kg is null;
alter table storage_cells alter column warehouse_id set not null;
alter table storage_cells alter column max_weight_kg set default 0;
alter table storage_cells alter column max_weight_kg set not null;
alter table storage_cells alter column length_cm set default 0;
alter table storage_cells alter column length_cm set not null;
alter table storage_cells alter column width_cm set default 0;
alter table storage_cells alter column width_cm set not null;
alter table storage_cells alter column height_cm set default 0;
alter table storage_cells alter column height_cm set not null;
alter table storage_cells alter column current_weight_kg set default 0;
alter table storage_cells alter column current_weight_kg set not null;

alter table counterparties add column if not exists code varchar(50);
alter table counterparties add column if not exists gln varchar(50);
alter table counterparties add column if not exists email varchar(150);
alter table counterparties add column if not exists phone varchar(50);
alter table counterparties add column if not exists address varchar(255);
alter table counterparties add column if not exists is_active boolean not null default true;
alter table counterparties add column if not exists created_at timestamp not null default current_timestamp;
alter table counterparties add column if not exists updated_at timestamp not null default current_timestamp;
update counterparties set code = 'CP-' || id where code is null;
alter table counterparties alter column code set not null;

alter table operations add column if not exists source varchar(30) not null default 'MANUAL';
alter table operations add column if not exists external_document_number varchar(100);
alter table operations add column if not exists document_date date;
alter table operations add column if not exists created_by_user_id bigint;
alter table operations add column if not exists completed_by_user_id bigint;
alter table operations add column if not exists comment varchar(255);
do $$
begin
    if exists (
        select 1 from information_schema.columns
        where table_schema = 'public' and table_name = 'operations' and column_name = 'user_id'
    ) then
        update operations set created_by_user_id = user_id where created_by_user_id is null and user_id is not null;
    end if;
end $$;
update operations set created_by_user_id = (select min(id) from users) where created_by_user_id is null;
alter table operations alter column created_by_user_id set not null;

alter table operation_items add column if not exists unit_price numeric(14, 2) not null default 0;
alter table operation_items add column if not exists unit_of_measure varchar(20) not null default 'pcs';
alter table stock_balances add column if not exists reserved_quantity integer not null default 0;
alter table stock_balances add column if not exists updated_at timestamp not null default current_timestamp;

create table if not exists audit_log (
    id bigserial primary key,
    entity_name varchar(120) not null,
    entity_id varchar(120) not null,
    action varchar(60) not null,
    username varchar(100) not null,
    occurred_at timestamp not null default current_timestamp,
    details_json jsonb
);

create table if not exists edi_partners (
    id bigserial primary key,
    code varchar(50) not null unique,
    name varchar(255) not null,
    gln varchar(50),
    counterparty_id bigint references counterparties(id),
    default_warehouse_id bigint references warehouses(id),
    inbound_enabled boolean not null default true,
    outbound_enabled boolean not null default false,
    is_active boolean not null default true,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create table if not exists edi_messages (
    id bigserial primary key,
    message_type varchar(30) not null,
    direction varchar(30) not null,
    status varchar(30) not null,
    interchange_ref varchar(100),
    message_ref varchar(100),
    document_number varchar(100),
    raw_payload text,
    normalized_payload jsonb,
    partner_id bigint references edi_partners(id),
    related_operation_id bigint references operations(id),
    received_at timestamp not null default current_timestamp,
    processed_at timestamp,
    error_message text
);

create table if not exists edi_mapping_config (
    id bigserial primary key,
    partner_id bigint not null references edi_partners(id),
    message_type varchar(30) not null,
    external_product_code varchar(100) not null,
    external_uom varchar(20),
    internal_product_id bigint not null references products(id),
    internal_uom varchar(20) not null default 'pcs',
    is_active boolean not null default true,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create table if not exists edi_processing_queue (
    id bigserial primary key,
    edi_message_id bigint not null unique references edi_messages(id) on delete cascade,
    status varchar(30) not null,
    attempt_count integer not null default 0,
    scheduled_at timestamp not null default current_timestamp,
    started_at timestamp,
    finished_at timestamp,
    last_error text
);

create table if not exists edi_audit_log (
    id bigserial primary key,
    edi_message_id bigint not null references edi_messages(id) on delete cascade,
    stage varchar(80) not null,
    status varchar(30) not null,
    details text,
    created_at timestamp not null default current_timestamp
);

create table if not exists revinfo (
    rev integer generated by default as identity primary key,
    revtstmp bigint
);

create table if not exists users_aud (
    id bigint not null,
    rev integer not null references revinfo(rev),
    revtype smallint,
    username varchar(100),
    password_hash varchar(255),
    full_name varchar(255),
    email varchar(150),
    role varchar(30),
    is_active boolean,
    created_at timestamp,
    updated_at timestamp,
    primary key (id, rev)
);

create table if not exists products_aud (
    id bigint not null,
    rev integer not null references revinfo(rev),
    revtype smallint,
    sku varchar(100),
    barcode varchar(100),
    name varchar(255),
    category varchar(100),
    unit_of_measure varchar(20),
    min_stock_level integer,
    weight_per_unit_kg numeric(12, 3),
    volume_per_unit_cm3 numeric(14, 3),
    length_cm numeric(10, 2),
    width_cm numeric(10, 2),
    height_cm numeric(10, 2),
    is_active boolean,
    created_at timestamp,
    updated_at timestamp,
    primary key (id, rev)
);

create table if not exists warehouses_aud (
    id bigint not null,
    rev integer not null references revinfo(rev),
    revtype smallint,
    code varchar(50),
    name varchar(255),
    address varchar(255),
    is_active boolean,
    created_at timestamp,
    updated_at timestamp,
    primary key (id, rev)
);

create table if not exists storage_cells_aud (
    id bigint not null,
    rev integer not null references revinfo(rev),
    revtype smallint,
    warehouse_id bigint,
    code varchar(50),
    zone varchar(50),
    rack varchar(50),
    shelf varchar(50),
    level varchar(50),
    capacity_units integer,
    max_weight_kg numeric(12, 3),
    max_volume_cm3 numeric(14, 3),
    length_cm numeric(10, 2),
    width_cm numeric(10, 2),
    height_cm numeric(10, 2),
    current_weight_kg numeric(12, 3),
    current_volume_cm3 numeric(14, 3),
    is_active boolean,
    created_at timestamp,
    updated_at timestamp,
    primary key (id, rev)
);

create table if not exists counterparties_aud (
    id bigint not null,
    rev integer not null references revinfo(rev),
    revtype smallint,
    code varchar(50),
    name varchar(255),
    type varchar(30),
    tax_id varchar(50),
    gln varchar(50),
    email varchar(150),
    phone varchar(50),
    address varchar(255),
    contact_info varchar(255),
    is_active boolean,
    created_at timestamp,
    updated_at timestamp,
    primary key (id, rev)
);

create table if not exists operations_aud (
    id bigint not null,
    rev integer not null references revinfo(rev),
    revtype smallint,
    operation_number varchar(80),
    type varchar(30),
    status varchar(30),
    warehouse_id bigint,
    counterparty_id bigint,
    source varchar(30),
    external_document_number varchar(100),
    document_date date,
    created_by_user_id bigint,
    completed_by_user_id bigint,
    comment varchar(255),
    created_at timestamp,
    completed_at timestamp,
    primary key (id, rev)
);

create table if not exists operation_items_aud (
    id bigint not null,
    rev integer not null references revinfo(rev),
    revtype smallint,
    operation_id bigint,
    product_id bigint,
    quantity integer,
    unit_price numeric(14, 2),
    unit_of_measure varchar(20),
    from_cell_id bigint,
    to_cell_id bigint,
    primary key (id, rev)
);

create table if not exists stock_balances_aud (
    product_id bigint not null,
    cell_id bigint not null,
    rev integer not null references revinfo(rev),
    revtype smallint,
    quantity integer,
    reserved_quantity integer,
    updated_at timestamp,
    primary key (product_id, cell_id, rev)
);

create unique index if not exists uk_products_barcode on products(barcode);
create unique index if not exists uk_warehouses_code on warehouses(code);
create unique index if not exists uk_counterparties_code on counterparties(code);
create unique index if not exists uk_storage_cells_warehouse_code_idx on storage_cells(warehouse_id, code);
create index if not exists idx_counterparties_gln on counterparties(gln);
create index if not exists idx_operations_type_status_created_at on operations(type, status, created_at);
create index if not exists idx_operations_warehouse_created_at on operations(warehouse_id, created_at);
create index if not exists idx_operations_counterparty on operations(counterparty_id);
create index if not exists idx_operation_items_operation on operation_items(operation_id);
create index if not exists idx_operation_items_product on operation_items(product_id);
create index if not exists idx_stock_balances_cell on stock_balances(cell_id);
create index if not exists idx_edi_messages_type_status_received_at on edi_messages(message_type, status, received_at);
create index if not exists idx_edi_messages_message_ref on edi_messages(message_ref);
create index if not exists idx_edi_processing_queue_status_scheduled_at on edi_processing_queue(status, scheduled_at);
