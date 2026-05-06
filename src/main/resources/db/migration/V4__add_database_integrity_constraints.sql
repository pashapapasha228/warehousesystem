update users set role = replace(role, 'ROLE_', '') where role like 'ROLE_%';
update users set role = 'STOREKEEPER' where role is null or role not in ('ADMIN', 'MANAGER', 'STOREKEEPER');

update products set min_stock_level = 0 where min_stock_level < 0;
update products set weight_per_unit_kg = 0 where weight_per_unit_kg < 0;
update products set volume_per_unit_cm3 = 0 where volume_per_unit_cm3 < 0;
update products set length_cm = 0 where length_cm < 0;
update products set width_cm = 0 where width_cm < 0;
update products set height_cm = 0 where height_cm < 0;

update storage_cells set capacity_units = 0 where capacity_units < 0;
update storage_cells set max_weight_kg = 0 where max_weight_kg < 0;
update storage_cells set max_volume_cm3 = 0 where max_volume_cm3 < 0;
update storage_cells set length_cm = 0 where length_cm < 0;
update storage_cells set width_cm = 0 where width_cm < 0;
update storage_cells set height_cm = 0 where height_cm < 0;
update storage_cells set current_weight_kg = 0 where current_weight_kg < 0;
update storage_cells set current_volume_cm3 = 0 where current_volume_cm3 < 0;
update storage_cells
set current_weight_kg = max_weight_kg
where max_weight_kg > 0 and current_weight_kg > max_weight_kg;
update storage_cells
set current_volume_cm3 = max_volume_cm3
where max_volume_cm3 > 0 and current_volume_cm3 > max_volume_cm3;

update operations set status = 'DRAFT' where status is null or status not in ('DRAFT', 'COMPLETED', 'CANCELLED');
update operations set source = 'MANUAL' where source is null or source not in ('MANUAL', 'EDI');

update operation_items set quantity = 1 where quantity is null or quantity <= 0;
update operation_items set unit_price = 0 where unit_price is null or unit_price < 0;
update operation_items set unit_of_measure = 'pcs' where unit_of_measure is null;

update stock_balances set quantity = 0 where quantity is null or quantity < 0;
update stock_balances set reserved_quantity = 0 where reserved_quantity is null or reserved_quantity < 0;
update stock_balances set reserved_quantity = quantity where reserved_quantity > quantity;

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'ck_users_role') then
        alter table users add constraint ck_users_role
            check (role in ('ADMIN', 'MANAGER', 'STOREKEEPER'));
    end if;
end $$;

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'ck_products_non_negative_numbers') then
        alter table products add constraint ck_products_non_negative_numbers
            check (
                min_stock_level >= 0
                and weight_per_unit_kg >= 0
                and volume_per_unit_cm3 >= 0
                and length_cm >= 0
                and width_cm >= 0
                and height_cm >= 0
            );
    end if;
end $$;

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'ck_storage_cells_non_negative_numbers') then
        alter table storage_cells add constraint ck_storage_cells_non_negative_numbers
            check (
                capacity_units >= 0
                and max_weight_kg >= 0
                and max_volume_cm3 >= 0
                and length_cm >= 0
                and width_cm >= 0
                and height_cm >= 0
                and current_weight_kg >= 0
                and current_volume_cm3 >= 0
            );
    end if;
end $$;

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'ck_storage_cells_current_limits') then
        alter table storage_cells add constraint ck_storage_cells_current_limits
            check (
                (max_weight_kg = 0 or current_weight_kg <= max_weight_kg)
                and (max_volume_cm3 = 0 or current_volume_cm3 <= max_volume_cm3)
            );
    end if;
end $$;

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'ck_counterparties_type') then
        alter table counterparties add constraint ck_counterparties_type
            check (type in ('SUPPLIER', 'CUSTOMER'));
    end if;
end $$;

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'ck_operations_type') then
        alter table operations add constraint ck_operations_type
            check (type in ('INCOME', 'OUTCOME', 'MOVE'));
    end if;
end $$;

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'ck_operations_status') then
        alter table operations add constraint ck_operations_status
            check (status in ('DRAFT', 'COMPLETED', 'CANCELLED'));
    end if;
end $$;

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'ck_operations_source') then
        alter table operations add constraint ck_operations_source
            check (source in ('MANUAL', 'EDI'));
    end if;
end $$;

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'ck_operation_items_positive_quantity') then
        alter table operation_items add constraint ck_operation_items_positive_quantity
            check (quantity > 0);
    end if;
end $$;

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'ck_operation_items_non_negative_price') then
        alter table operation_items add constraint ck_operation_items_non_negative_price
            check (unit_price >= 0);
    end if;
end $$;

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'ck_stock_balances_quantities') then
        alter table stock_balances add constraint ck_stock_balances_quantities
            check (quantity >= 0 and reserved_quantity >= 0 and reserved_quantity <= quantity);
    end if;
end $$;

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'ck_edi_messages_type') then
        alter table edi_messages add constraint ck_edi_messages_type
            check (message_type in ('ORDERS', 'DESADV', 'ORDRSP'));
    end if;
end $$;

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'ck_edi_messages_direction') then
        alter table edi_messages add constraint ck_edi_messages_direction
            check (direction in ('INBOUND', 'OUTBOUND'));
    end if;
end $$;

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'ck_edi_messages_status') then
        alter table edi_messages add constraint ck_edi_messages_status
            check (status in ('RECEIVED', 'NORMALIZED', 'PROCESSING', 'PROCESSED', 'FAILED'));
    end if;
end $$;

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'ck_edi_mapping_config_message_type') then
        alter table edi_mapping_config add constraint ck_edi_mapping_config_message_type
            check (message_type in ('ORDERS', 'DESADV', 'ORDRSP'));
    end if;
end $$;

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'ck_edi_processing_queue_status') then
        alter table edi_processing_queue add constraint ck_edi_processing_queue_status
            check (status in ('PENDING', 'RUNNING', 'DONE', 'FAILED'));
    end if;
end $$;

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'ck_edi_processing_queue_attempt_count') then
        alter table edi_processing_queue add constraint ck_edi_processing_queue_attempt_count
            check (attempt_count >= 0);
    end if;
end $$;

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'ck_edi_audit_log_status') then
        alter table edi_audit_log add constraint ck_edi_audit_log_status
            check (status in ('SUCCESS', 'FAILED', 'SKIPPED'));
    end if;
end $$;

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'fk_operations_created_by_user') then
        alter table operations add constraint fk_operations_created_by_user
            foreign key (created_by_user_id) references users(id);
    end if;
end $$;

do $$
begin
    if not exists (select 1 from pg_constraint where conname = 'fk_operations_completed_by_user') then
        alter table operations add constraint fk_operations_completed_by_user
            foreign key (completed_by_user_id) references users(id);
    end if;
end $$;

create index if not exists idx_audit_log_entity_occurred_at
    on audit_log(entity_name, entity_id, occurred_at);

create index if not exists idx_audit_log_username_occurred_at
    on audit_log(username, occurred_at);

create index if not exists idx_edi_audit_log_message_created_at
    on edi_audit_log(edi_message_id, created_at);

create index if not exists idx_edi_mapping_config_partner_product
    on edi_mapping_config(partner_id, external_product_code, is_active);
