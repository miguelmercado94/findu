# Informe: finanzas del proveedor y liquidacion simulada

## 1. Formulas

Neto (unica fuente: `SettlementBalanceCalculator`, usada por `PendingBalanceService`, resumen semanal, movimientos y ejecucion):

```
compensacion   = min(bruto, deuda_vigente)
neto           = bruto - compensacion          (nunca negativo)
deuda_restante = deuda_vigente - compensacion
```

Ejemplo del caso reportado: bruto 186.300 - compensacion comision en efectivo 1.300 = neto 185.000. Dashboard e historial leen el mismo `pendingBalance`, por eso ya no hay 185 vs 186.

Fecha de proxima liquidacion (`SettlementSchedule`, zona America/Bogota): corte el domingo, pago el martes.
- Corte = domingo estrictamente anterior a la fecha de ejecucion.
- Ventana = (periodEnd de la ultima liquidacion COMPLETED, domingo de corte] inclusive, fin de dia.
- Proximo martes = primer martes estrictamente posterior a hoy (si hoy es martes, el de la semana siguiente).
- Si lo ganado queda despues del corte, no se pierde: se arrastra al martes siguiente (`carriedOver=true`). Esto explica "17 de noviembre" cuando lo ganado (26 oct) ya paso el martes cercano sin liquidarse: antes no habia proceso de liquidacion.
- Codigo idempotente de liquidacion: `SET-{providerId}-{yyyyMMdd del corte}`.

## 2. Esquema y migraciones (findu-transaction)

Idempotentes (`IF NOT EXISTS`), en `db/h2/schema.sql` y `db/postgres/schema.sql`:

```sql
ALTER TABLE daily_balances ADD COLUMN IF NOT EXISTS settlement_id VARCHAR(36);
ALTER TABLE daily_balances ADD COLUMN IF NOT EXISTS settled_at TIMESTAMP;
CREATE INDEX IF NOT EXISTS idx_daily_balances_settled ON daily_balances(provider_id, settled_at);
```

`settlement_daily_balances` ya existia. `payouts.settlement_id` UNIQUE respalda la idempotencia. La lambda `findu-transaction-daily-balance` ya no dispara corte dominical y no recalcula balances `SETTLED`.

## 3. Endpoints

Todos en `findu-transaction`. Los `/internal/settlements/**` exigen header `X-Internal-Service-Key` (fail-closed si la llave esta vacia; comparacion en tiempo constante); sin llave o incorrecta: 403.

Nuevos:

| Metodo | Path | Parametros | Respuesta |
|---|---|---|---|
| POST | `/internal/settlements/execute` | query opcionales `providerId` (Long), `cutoffDate` (ISO yyyy-MM-dd). Sin cuerpo | 200 `SettlementExecutionResultResponse`; 403 sin llave; 503 si hay neto > 0 y no hay pasarela de payout |

Respuesta de `execute` y `run`: `executionId, targetDate, cutoffDate, nextSettlementDate, providersProcessed, settlementsCreated, settlementsSkipped, providersFailed, startedAt, finishedAt, status, errors[], results[]`. Cada `results[]`: `providerId, outcome, settlementId, settlementCode, grossAmount, debtCompensationAmount, netAmount, remainingDebt, transferred, payoutCode, reference, periodStart, periodEnd, executedAt, dailyBalancesSettled, message`. `outcome` posibles: liquidada, `ALREADY_SETTLED` (mismo corte, sin pagar de nuevo), `NET_ZERO_NO_TRANSFER` (neto <= 0, solo compensa deuda), `FAILED`. Referencia del mock: `MOCK-TRANSFER-<uuid>`.

Modificados:

| Metodo | Path | Cambio |
|---|---|---|
| POST | `/internal/settlements/run` (query `date`, `type`) | ahora exige llave; ejecuta lote de liquidaciones |
| POST | `/internal/settlements/provider/{providerId}` (query `date`, `type`) | ahora exige llave |
| POST | `/api/v1/settlements` | AHORA dispara la liquidacion completa con payout mock (antes solo creaba el registro). No lleva llave: depende de la proteccion del gateway (hallazgo del review, ver pendientes) |
| GET | `/api/v1/providers/{providerId}/finances/weekly-balance` | agrega `pendingBalance`, `nextSettlement.estimatedAmount` (neto al corte del proximo martes), `cutoffDate`, ultima liquidacion transferida |
| GET | `/api/v1/providers/{providerId}/finances/movements` | agrega `pendingBalance`; cada item suma `movementType`, `description`, `settlementCode`; la comision en efectivo ya no se titula como el servicio; aparece `SETTLEMENT_PAYOUT` "Liquidacion transferida" |

`pendingBalance`: `grossAmount, debtCompensationAmount, netAmount, remainingDebt, thisWeekGrossAmount, carriedOverGrossAmount, carriedOver, pendingServicesCount, currency`.

Efecto contable de una liquidacion: asientos `COMPENSATION_ADJUSTMENT` y `PAYOUT` en el ledger, payout `MOCK-TRANSFER-*`, `daily_balances` marcados SETTLED, `pendingServicesCount` vuelve a 0.

Core (otro workflow, mismo repo, no es de finanzas): `GET /internal/perfiles/by-auth/{authUserId}` (llave interna) -> `{perfilId, authUserId}`, 404 si no existe.

## 4. Variables y flags

| Propiedad | Valor | Notas |
|---|---|---|
| `findu.settlement.mock.enabled` | `false` por defecto y en qa; `true` en local y dev | Habilita el job y el adaptador `MockPayoutGatewayAdapter`. Sin el, con neto > 0 la liquidacion falla (503 manual, FAILED en lote) |
| `findu.settlement.cron` | env `FINDU_SETTLEMENT_CRON`, default `0 0 6 * * TUE` | Martes 06:00 |
| `findu.settlement.zone` | `America/Bogota` | Zona del cron |
| `findu.internal.service-key` | env `FINDU_INTERNAL_SERVICE_KEY` | Default vacio (fail-closed). En local y dev trae un default en claro en el YAML (ver `application-local.yml`); en qa/prod sin default |

## 5. Disparar la liquidacion manual

Contenedor `findu-transaction`, perfil `local`, puerto interno 8086 (el de la ruta del gateway en docker-compose), curl disponible dentro del contenedor. El header es `X-Internal-Service-Key`; la llave es la de `FINDU_INTERNAL_SERVICE_KEY` o, si no esta definida, el default de `application-local.yml`.

Todos los proveedores:

```powershell
docker exec findu-transaction curl -s -X POST "http://localhost:8086/internal/settlements/execute" -H "X-Internal-Service-Key: <LLAVE>"
```

Un proveedor (y opcionalmente un corte concreto):

```powershell
docker exec findu-transaction curl -s -X POST "http://localhost:8086/internal/settlements/execute?providerId=<ID>&cutoffDate=2026-11-01" -H "X-Internal-Service-Key: <LLAVE>"
```

Repetir el mismo comando devuelve `ALREADY_SETTLED` y no paga de nuevo.

Advertencia verificada: el contenedor `findu-transaction` actual fue creado el 2026-11-09 14:07 UTC y, al probar `POST /internal/settlements/execute` sin llave, respondio 500 en lugar de 403. Eso sugiere que la imagen en ejecucion NO incluye aun el codigo nuevo. Por instruccion no se reconstruyo ni se reinicio nada; antes de la prueba hay que reconstruir `findu-transaction` (y la lambda daily-balance). No se probo el endpoint con llave.

## 6. Resultados de verificacion (tomados de mensajes de commit y reviews; no se re-ejecutaron)

Backend (`findu-transaction`):
- `gradlew clean compileJava -x test`: OK.
- `gradlew test --offline`: 59 tests, 58 OK, 1 falla baseline ajena (`FinduTransactionApplicationTests.contextLoads`, Postgres UnknownHost).
- Lambda `findu-transaction-daily-balance` `gradlew clean test --offline`: BUILD SUCCESSFUL.
- Review feat2: APPROVED.

App (`findu_proveedores`, finanzas):
- `flutter pub get` OK. `flutter analyze`: 40 issues, 0 errores, sin nuevos en finanzas.
- `flutter test`: 214 pasan, 4 fallan, previos y ajenos (alert_websocket_mapping, biometric_capture_flow, profile_alert_pull, profile_and_navigation_redesign). `finances_settlement_consistency_test`: 18/18.
- `flutter build apk --debug`: OK.
- Review feat3: APPROVED.

Iconos de lapiz (commit de86c46): analyze 39 issues (baseline), 148 pasan, 4 fallos previos ajenos. Review feat1: APPROVED. No se re-ejecuto nada en este cierre.

## 7. Commits

Backend (`findu`, rama `develop`):
- `2f5aacf` feat(transaction): saldo neto unico (PendingBalanceService) y movimientos claros
- `49bd02e` feat(transaction): liquidacion simulada idempotente + puerto PayoutGatewayPort (mock)
- `4167f3d` feat(transaction): POST /internal/settlements/execute con llave y job de martes configurable
- `2f8a777` test(transaction): neto unico, liquidacion ejemplo, idempotencia, arrastre, net<=0, llave y flag del job
- Hash de push: ver seccion 8.

App (`findu-movil/apps/findu_proveedores`, rama `develop`, sin remoto, no pusheado):
- `de86c46` refactor(profile): iconos de lapiz y retiro del bloque de correccion
- `1c6fa75` feat(finances): saldo neto unico, arrastre y liquidaciones en historial
- `c08df60` fix(finances): cifra principal del historial = neto pendiente, deuda y rotulos

## 8. Estado del push

Ver mensaje final del workflow (se completa tras `git push origin develop`).

## 9. Pendientes

- Transferencia real (Bre-B o banco): hoy solo existe el mock. Un adaptador real asincrono exigira estado intermedio y outbox (`PayoutGatewayPort` esta separado de `BreBPaymentGatewayPort`).
- Conciliacion: contrastar payouts con el extracto bancario; hoy no existe.
- Zona horaria: el corte usa America/Bogota en backend; revisar que app y lambda daily-balance usen el mismo limite de dia.
- Reintentos: si la pasarela falla se hace rollback completo; no hay reintento automatico ni cola de fallidos.
- `POST /api/v1/settlements` sin llave interna ahora mueve dinero (mock): protegerlo en el gateway o exigir llave.
- Llave interna con default en claro en `application-local.yml` y `application-dev.yml`: en dev compartido usar `FINDU_INTERNAL_SERVICE_KEY`.
- Falta una asercion explicita sobre las filas de `settlement_daily_balances`.
- Estado de deuda: el titular del dashboard (`provisionalBalance`, negativo) difiere de "Neto a liquidar" del historial (0). Decision de producto pendiente.
- Fallback de la app a `netBalance` del ledger cuando falta `pendingBalance` (backend antiguo); retirarlo cuando todos los entornos tengan el contrato nuevo.
- 4 tests de la app y `contextLoads` del backend fallan de base, sin relacion con este trabajo.

## 10. Plan de prueba manual en el celular

Prerrequisito: reconstruir y levantar `findu-transaction` con el codigo nuevo (ver advertencia en seccion 5) e instalar el APK debug de la app apuntando al backend.

Antes de liquidar:
1. Entrar como el proveedor de prueba y abrir Finanzas (dashboard).
2. Verificar neto 185.000 con el desglose: ganancias 186.300, menos compensacion de comisiones en efectivo 1.300, igual 185.000.
3. Si hay lo ganado el 26 de octubre sin liquidar, debe verse como "Saldo arrastrado de semanas anteriores" y la tarjeta "Proxima liquidacion" con un martes futuro (por ejemplo el 17 de noviembre) y rotulo "Neto del proximo corte".
4. Abrir el historial: la cifra principal "Neto a liquidar" debe ser 185.000, igual al dashboard. Debe aparecer la comision en efectivo como tal y no como el servicio.
5. Ver que Informacion personal muestra lapices de edicion y no tiene el bloque azul de datos incorrectos.

Liquidar (mock):
6. Ejecutar el curl de la seccion 5 con `providerId` del proveedor. Esperado: `outcome` de liquidada, `netAmount` 185000, `transferred=true`, `payoutCode`/`reference` `MOCK-TRANSFER-...`.
7. Repetir el mismo curl: esperado `ALREADY_SETTLED`, sin nuevo pago.

Despues de liquidar:
8. Refrescar el dashboard: saldo/neto 0, desglose en cero, pendientes de liquidar en 0, y la tarjeta de proxima liquidacion indica la ultima liquidacion transferida con su referencia.
9. Historial: aparece el movimiento "Liquidacion transferida" con la referencia `MOCK-TRANSFER-...`, sin navegacion al detalle, y "Neto a liquidar" en 0.
10. Si se completa un servicio nuevo despues, el nuevo saldo debe acumularse desde cero y programarse para el martes siguiente.
