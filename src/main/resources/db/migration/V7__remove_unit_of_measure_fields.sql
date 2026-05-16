alter table products drop column if exists unit_of_measure;
alter table products_aud drop column if exists unit_of_measure;

alter table operation_items drop column if exists unit_of_measure;
alter table operation_items_aud drop column if exists unit_of_measure;

alter table edi_mapping_config drop column if exists external_uom;
alter table edi_mapping_config drop column if exists internal_uom;

update edi_messages
set normalized_payload = jsonb_set(
        normalized_payload,
        '{items}',
        (
            select coalesce(jsonb_agg(item - 'unitOfMeasure'), '[]'::jsonb)
            from jsonb_array_elements(normalized_payload -> 'items') as item
        )
    )
where normalized_payload ? 'items'
  and jsonb_typeof(normalized_payload -> 'items') = 'array';

update edi_messages
set raw_payload = regexp_replace(
        regexp_replace(raw_payload, ',\s*"unitOfMeasure"\s*:\s*"[^"]*"', '', 'g'),
        '"unitOfMeasure"\s*:\s*"[^"]*"\s*,',
        '',
        'g'
    )
where raw_payload is not null
  and raw_payload like '%unitOfMeasure%';
