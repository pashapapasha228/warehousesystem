# EDI process MVP

EDI is an external document exchange layer. It stores inbound/outbound documents, validates them, and links them to internal warehouse operations when the business scenario allows it. Stock balances are changed only by `OperationService.completeOperation(...)`.

## Supported scenarios

- `DESADV + INBOUND + SUPPLIER`: supplier shipment notice. Creates a draft `INCOME` operation. Each item must contain `toCellId`.
- `ORDERS + INBOUND + CUSTOMER`: customer order. Creates a draft `OUTCOME` operation. Each item must contain `fromCellId`, and available stock is checked before the draft is created.
- `ORDRSP`: order response. Stored and processed as an EDI document, but it does not create a warehouse operation.

Unsupported combinations fail processing with a clear error. Examples: supplier `ORDERS` does not become `OUTCOME`, and customer `DESADV` does not become `INCOME`.

## Supplier DESADV payload

```json
{
  "warehouseId": 1,
  "documentNumber": "ASN-1001",
  "documentDate": "2026-05-14",
  "items": [
    {
      "externalProductCode": "SUPPLIER-SKU-001",
      "quantity": 5,
      "toCellId": 1,
      "unitPrice": 10.0
    }
  ]
}
```

## Customer ORDERS payload

```json
{
  "warehouseId": 1,
  "documentNumber": "ORDER-2001",
  "documentDate": "2026-05-14",
  "items": [
    {
      "externalProductCode": "CUSTOMER-SKU-001",
      "quantity": 2,
      "fromCellId": 1,
      "unitPrice": 10.0
    }
  ]
}
```

The `externalProductCode` must have an active `EdiMappingConfig` for the partner and message type. `warehouseId` can be omitted only when the EDI partner has a default warehouse.
