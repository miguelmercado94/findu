package com.findu.notification.processor.application.port.in;

import com.findu.notification.processor.application.dto.request.ActivateVersionRequest;
import com.findu.notification.processor.application.dto.request.CreateTemplateRequest;
import com.findu.notification.processor.application.dto.request.UpdateTemplateRequest;
import com.findu.notification.processor.application.dto.response.TemplateResponse;
import com.findu.notification.processor.application.dto.response.TemplateVersionResponse;
import com.findu.notification.processor.domain.enums.TemplateType;

import java.util.List;

/**
 * Caso de uso de administración de plantillas (diseño §10, superficie admin).
 *
 * <p>Permite a un rol admin crear, editar (generando una versión nueva inmutable),
 * activar/rollback de una versión previa, listar por tipo, consultar el histórico
 * y desactivar una plantilla. La edición nunca sobreescribe: cada cambio produce
 * una nueva versión {@code v#<n+1>} y repunta el {@code v#ACTIVE}.</p>
 */
public interface ManageTemplateUseCase {

    /**
     * Crea una plantilla nueva (versión 1 si no existe) escribiendo el ítem inmutable
     * y el puntero activo en una sola transacción.
     */
    TemplateResponse create(CreateTemplateRequest request);

    /**
     * Edita una plantilla existente generando una <b>versión nueva</b> (nunca sobreescribe)
     * y repuntando el {@code v#ACTIVE} a esa versión.
     */
    TemplateResponse edit(String codigo, TemplateType tipo, String language, UpdateTemplateRequest request);

    /**
     * Activa (o hace rollback a) una versión previa, repuntando el {@code v#ACTIVE}.
     */
    TemplateResponse activateVersion(ActivateVersionRequest request);

    /**
     * Lista la versión activa de las plantillas de un tipo funcional.
     */
    List<TemplateResponse> listByType(TemplateType tipo);

    /**
     * Devuelve el histórico de versiones inmutables de una plantilla.
     */
    List<TemplateVersionResponse> history(String codigo, TemplateType tipo, String language);

    /**
     * Desactiva la versión activa de una plantilla ({@code is_active = false}).
     */
    void deactivate(String codigo, TemplateType tipo, String language);
}
