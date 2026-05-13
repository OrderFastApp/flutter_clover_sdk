# Changelog

## 0.8.10

### printer / model

- `NonFiscalTicket`: campos `subtotalBeforeDiscount` y `discountAppliedAmount` para totales del pie ya calculados desde Flutter; el pie deja de inferir montos desde porcentaje/monto informativo cuando enviás ambos valores.
- `printTicket`, `printTextTicket`, `printCashTicket`: el mapa `nonFiscalTicket` acepta las nuevas claves; `orderToNonFiscalTicket` las rellena cuando la suma de ítems es mayor que el total de la orden.

## 0.6.5

### printer
- Mejoro sistema de tickets


## 0.6.3

### printer
- Added font roboto


## 0.6.1

### printer
- Fixed font size


## 0.6.0

### printer
- Added printerJob for text


## 0.0.1

* TODO: Describe initial release.
