create table users (
    id bigserial primary key,
    username varchar(100) not null unique,
    password_hash varchar(255) not null,
    full_name varchar(255),
    email varchar(150),
    role varchar(30) not null,
    is_active boolean not null default true,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create table products (
    id bigserial primary key,
    sku varchar(100) not null unique,
    barcode varchar(100) unique,
    name varchar(255) not null,
    category varchar(100),
    unit_of_measure varchar(20) not null default 'pcs',
    min_stock_level integer not null default 0,
    weight_per_unit_kg numeric(12, 3) not null default 0,
    volume_per_unit_cm3 numeric(14, 3) not null default 0,
    length_cm numeric(10, 2) not null default 0,
    width_cm numeric(10, 2) not null default 0,
    height_cm numeric(10, 2) not null default 0,
    is_active boolean not null default true,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create table warehouses (
    id bigserial primary key,
    code varchar(50) not null unique,
    name varchar(255) not null,
    address varchar(255),
    is_active boolean not null default true,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create table storage_cells (
    id bigserial primary key,
    warehouse_id bigint not null references warehouses(id),
    code varchar(50) not null,
    zone varchar(50),
    rack varchar(50),
    shelf varchar(50),
    level varchar(50),
    capacity_units integer not null default 0,
    max_weight_kg numeric(12, 3) not null default 0,
    max_volume_cm3 numeric(14, 3) not null default 0,
    length_cm numeric(10, 2) not null default 0,
    width_cm numeric(10, 2) not null default 0,
    height_cm numeric(10, 2) not null default 0,
    current_weight_kg numeric(12, 3) not null default 0,
    current_volume_cm3 numeric(14, 3) not null default 0,
    is_active boolean not null default true,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp,
    constraint uk_storage_cells_warehouse_code unique (warehouse_id, code)
);

create table counterparties (
    id bigserial primary key,
    code varchar(50) not null unique,
    name varchar(255) not null,
    type varchar(30) not null,
    tax_id varchar(50),
    gln varchar(50),
    email varchar(150),
    phone varchar(50),
    address varchar(255),
    contact_info varchar(255),
    is_active boolean not null default true,
    created_at timestamp not null default current_timestamp,
    updated_at timestamp not null default current_timestamp
);

create table operations (
    id bigserial primary key,
    operation_number varchar(80) not null unique,
    type varchar(30) not null,
    status varchar(30) not null,
    warehouse_id bigint not null references warehouses(id),
    counterparty_id bigint references counterparties(id),
    source varchar(30) not null default 'MANUAL',
    external_document_number varchar(100),
    document_date date,
    created_by_user_id bigint not null references users(id),
    completed_by_user_id bigint references users(id),
    comment varchar(255),
    created_at timestamp not null default current_timestamp,
    completed_at timestamp
);

create table operation_items (
    id bigserial primary key,
    operation_id bigint not null references operations(id) on delete cascade,
    product_id bigint not null references products(id),
    quantity integer not null,
    unit_price numeric(14, 2) not null default 0,
    unit_of_measure varchar(20) not null default 'pcs',
    from_cell_id bigint references storage_cells(id),
    to_cell_id bigint references storage_cells(id)
);

create table stock_balances (
    product_id bigint not null references products(id),
    cell_id bigint not null references storage_cells(id),
    quantity integer not null default 0,
    reserved_quantity integer not null default 0,
    updated_at timestamp not null default current_timestamp,
    primary key (product_id, cell_id)
);

create table audit_log (
    id bigserial primary key,
    entity_name varchar(120) not null,
    entity_id varchar(120) not null,
    action varchar(60) not null,
    username varchar(100) not null,
    occurred_at timestamp not null default current_timestamp,
    details_json jsonb
);

create table edi_partners (
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

create table edi_messages (
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

create table edi_mapping_config (
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

create table edi_processing_queue (
    id bigserial primary key,
    edi_message_id bigint not null unique references edi_messages(id) on delete cascade,
    status varchar(30) not null,
    attempt_count integer not null default 0,
    scheduled_at timestamp not null default current_timestamp,
    started_at timestamp,
    finished_at timestamp,
    last_error text
);

create table edi_audit_log (
    id bigserial primary key,
    edi_message_id bigint not null references edi_messages(id) on delete cascade,
    stage varchar(80) not null,
    status varchar(30) not null,
    details text,
    created_at timestamp not null default current_timestamp
);

create table revinfo (
    rev integer generated by default as identity primary key,
    revtstmp bigint
);

create sequence revinfo_seq start with 1 increment by 50;

create table users_aud (
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

create table products_aud (
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

create table warehouses_aud (
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

create table storage_cells_aud (
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

create table counterparties_aud (
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

create table operations_aud (
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

create table operation_items_aud (
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

create table stock_balances_aud (
    product_id bigint not null,
    cell_id bigint not null,
    rev integer not null references revinfo(rev),
    revtype smallint,
    quantity integer,
    reserved_quantity integer,
    updated_at timestamp,
    primary key (product_id, cell_id, rev)
);

create index idx_products_barcode on products(barcode);
create index idx_counterparties_gln on counterparties(gln);
create index idx_operations_type_status_created_at on operations(type, status, created_at);
create index idx_operations_warehouse_created_at on operations(warehouse_id, created_at);
create index idx_operations_counterparty on operations(counterparty_id);
create index idx_operation_items_operation on operation_items(operation_id);
create index idx_operation_items_product on operation_items(product_id);
create index idx_stock_balances_cell on stock_balances(cell_id);
create index idx_edi_messages_type_status_received_at on edi_messages(message_type, status, received_at);
create index idx_edi_messages_message_ref on edi_messages(message_ref);
create index idx_edi_processing_queue_status_scheduled_at on edi_processing_queue(status, scheduled_at);
