package com.findu.help.v2.infrastructure.adapter.out.persistence;

import com.findu.help.v2.application.port.out.persistence.HelpCategoryRepository;
import com.findu.help.v2.domain.enums.PriorityLevel;
import com.findu.help.v2.domain.enums.UserRole;
import com.findu.help.v2.domain.model.category.HelpCategory;
import com.findu.help.v2.domain.model.category.HelpSubcategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HelpCategoryDataSeeder implements CommandLineRunner {

    private final HelpCategoryRepository helpCategoryRepository;

    @Override
    public void run(String... args) {
        if (!helpCategoryRepository.findAllActive().isEmpty()) {
            log.info("Categorías de ayuda ya inicializadas en MongoDB.");
            return;
        }

        log.info("Inicializando categorías y subcategorías base de soporte en MongoDB...");

        // 1. Finanzas y Pagos
        HelpCategory finanzas = HelpCategory.builder()
                .id("cat-finanzas-pagos")
                .name("Finanzas y Pagos")
                .code("FINANZAS")
                .description("Problemas con pagos, comisiones, cuentas o retiros.")
                .icon("wallet")
                .targetRole(UserRole.AMBOS)
                .order(1)
                .isActive(true)
                .subcategories(List.of(
                        new HelpSubcategory("sub-pagos-transferencias", "Pagos y transferencias", "El pago no llegó, error en la transacción.", false, PriorityLevel.ALTA, 80, true),
                        new HelpSubcategory("sub-comisiones", "Comisiones", "Duda sobre comisión aplicada.", false, PriorityLevel.MEDIA, 50, true),
                        new HelpSubcategory("sub-saldo-cuenta", "Saldo y cuenta", "Problemas con tu saldo o cuenta bancaria.", false, PriorityLevel.ALTA, 75, true),
                        new HelpSubcategory("sub-retiros", "Retiros", "No puedo retirar mis fondos.", false, PriorityLevel.CRITICA, 90, true),
                        new HelpSubcategory("sub-otros-finanzas", "Otros", "No encuentro mi problema en la lista.", false, PriorityLevel.MEDIA, 50, true)
                ))
                .build();

        // 2. Servicio o Cliente
        HelpCategory servicio = HelpCategory.builder()
                .id("cat-servicio-cliente")
                .name("Servicio o Cliente")
                .code("SERVICIO")
                .description("Incidentes con tus clientes, solicitudes o perfil.")
                .icon("customer-service")
                .targetRole(UserRole.AMBOS)
                .order(2)
                .isActive(true)
                .subcategories(List.of(
                        new HelpSubcategory("sub-incidentes-cliente", "Incidentes con el cliente", "Desacuerdo, cliente ausente o reporte de conducta.", true, PriorityLevel.ALTA, 80, true),
                        new HelpSubcategory("sub-cancelaciones", "Cancelaciones", "Cancelación involuntaria o fuera de tiempo.", true, PriorityLevel.MEDIA, 60, true),
                        new HelpSubcategory("sub-calificaciones", "Calificaciones o reseñas", "Disputa de calificación recibida.", false, PriorityLevel.BAJA, 30, true),
                        new HelpSubcategory("sub-otros-servicio", "Otros", "Otros problemas relacionados con servicios.", false, PriorityLevel.MEDIA, 50, true)
                ))
                .build();

        // 3. Perfil y Documentos
        HelpCategory perfil = HelpCategory.builder()
                .id("cat-perfil-documentos")
                .name("Perfil y Documentos")
                .code("PERFIL")
                .description("Actualiza tu información, documentos o verificación.")
                .icon("user-settings")
                .targetRole(UserRole.AMBOS)
                .order(3)
                .isActive(true)
                .subcategories(List.of(
                        new HelpSubcategory("sub-verificacion-identidad", "Verificación de identidad", "Documentos pendientes de validación o rechazados.", false, PriorityLevel.ALTA, 70, true),
                        new HelpSubcategory("sub-actualizacion-datos", "Actualización de datos", "Cambio de teléfono, correo o dirección.", false, PriorityLevel.MEDIA, 40, true),
                        new HelpSubcategory("sub-certificaciones", "Certificaciones y antecedentes", "Carga de nuevos certificados o antecedentes judiciales.", false, PriorityLevel.MEDIA, 50, true),
                        new HelpSubcategory("sub-otros-perfil", "Otros", "Otras consultas de perfil.", false, PriorityLevel.BAJA, 30, true)
                ))
                .build();

        // 4. Problemas Técnicos
        HelpCategory tecnico = HelpCategory.builder()
                .id("cat-problemas-tecnicos")
                .name("Problemas Técnicos")
                .code("TECNICO")
                .description("Errores en la app, rendimiento, conexiones o acceso.")
                .icon("phone-warning")
                .targetRole(UserRole.AMBOS)
                .order(4)
                .isActive(true)
                .subcategories(List.of(
                        new HelpSubcategory("sub-error-conexion", "Error de conexión o WebSocket", "No recibo notificaciones o solicitudes en tiempo real.", false, PriorityLevel.ALTA, 75, true),
                        new HelpSubcategory("sub-cierre-inesperado", "Cierre inesperado de la app", "La app se bloquea o cierra sola.", false, PriorityLevel.ALTA, 70, true),
                        new HelpSubcategory("sub-acceso-cuenta", "Problemas de acceso o login", "No puedo iniciar sesión o recuperar mi contraseña.", false, PriorityLevel.CRITICA, 90, true),
                        new HelpSubcategory("sub-otros-tecnico", "Otros", "Otro problema técnico.", false, PriorityLevel.MEDIA, 50, true)
                ))
                .build();

        helpCategoryRepository.save(finanzas);
        helpCategoryRepository.save(servicio);
        helpCategoryRepository.save(perfil);
        helpCategoryRepository.save(tecnico);

        log.info("4 categorías base de soporte inicializadas exitosamente con sus subcategorías.");
    }
}
