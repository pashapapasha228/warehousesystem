create table if not exists document_execution_steps (
    id bigserial primary key,
    operation_id bigint references operations(id) on delete cascade,
    edi_message_id bigint references edi_messages(id) on delete cascade,
    stage varchar(40) not null,
    status varchar(30) not null default 'DONE',
    details varchar(255),
    created_by varchar(100),
    created_at timestamp not null default current_timestamp
);

create table if not exists operation_verifications (
    id bigserial primary key,
    operation_id bigint not null references operations(id) on delete cascade,
    decision varchar(30) not null,
    comment varchar(255),
    verified_by varchar(100) not null,
    verified_at timestamp not null default current_timestamp
);

create table if not exists operation_verification_items (
    id bigserial primary key,
    verification_id bigint not null references operation_verifications(id) on delete cascade,
    operation_item_id bigint not null references operation_items(id) on delete cascade,
    planned_quantity integer not null,
    actual_quantity integer not null,
    discrepancy_quantity integer not null,
    reason varchar(255)
);

create index if not exists idx_document_execution_operation on document_execution_steps(operation_id, created_at);
create index if not exists idx_document_execution_edi_message on document_execution_steps(edi_message_id, created_at);
create index if not exists idx_operation_verifications_operation on operation_verifications(operation_id, verified_at);
