package com.findu.notification.processor.application.template;

import com.findu.notification.processor.application.port.out.externalapi.NotificationSenderPort;
import com.findu.notification.processor.application.service.TemplateService;
import com.findu.notification.processor.domain.enums.NotificationChannel;
import com.findu.notification.processor.domain.model.NotificationMessage;
import com.findu.notification.processor.domain.model.NotificationResult;
import com.findu.notification.processor.domain.model.NotificationTemplate;

import java.util.Map;
import java.util.Optional;

public class EmailNotificationTemplate extends AbstractNotificationTemplate {
    private final NotificationSenderPort sender;

    public EmailNotificationTemplate(NotificationSenderPort sender, TemplateService templateService) {
        super(templateService);
        this.sender = sender;
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    protected String resolveRecipient(NotificationMessage message) {
        return message.getRecipient();
    }

    @Override
    protected String buildSubject(NotificationMessage message) {
        Optional<NotificationTemplate> tpl = resolveTemplate(message);
        if (tpl.isPresent() && tpl.get().getSubject() != null && !tpl.get().getSubject().isBlank()) {
            String rawSubject = tpl.get().getSubject();
            if (message.getParams() != null) {
                for (Map.Entry<String, String> entry : message.getParams().entrySet()) {
                    rawSubject = rawSubject.replace("{{" + entry.getKey() + "}}", entry.getValue());
                }
            }
            return rawSubject;
        }

        if ("TICKET_RESOLVED".equalsIgnoreCase(message.getTemplateCode())) {
            String ticketCode = (message.getParams() != null && message.getParams().get("ticketCode") != null)
                    ? message.getParams().get("ticketCode")
                    : "";
            return "FindU Soporte - Tu ticket #" + ticketCode + " ha sido resuelto";
        }

        return super.buildSubject(message);
    }

    @Override
    protected String buildBody(NotificationMessage message) {
        Optional<NotificationTemplate> tpl = resolveTemplate(message);
        if (tpl.isPresent()) {
            return templateService.render(tpl.get(), message.getParams());
        }

        if ("TICKET_RESOLVED".equalsIgnoreCase(message.getTemplateCode())) {
            return renderDefaultTicketResolvedHtml(message.getParams());
        }

        return super.buildBody(message);
    }

    @Override
    protected NotificationResult doSend(String recipient, String subject, String body, Map<String, String> metadata) {
        log.info("[EMAIL] a={} subject={}", recipient, subject);
        return sender.send(recipient, subject, body, metadata);
    }

    private String renderDefaultTicketResolvedHtml(Map<String, String> params) {
        String ticketCode = getParam(params, "ticketCode", "N/A");
        String userName = getParam(params, "userName", "Usuario");
        String subject = getParam(params, "subject", "Ticket de Soporte");
        String actionType = getParam(params, "actionType", "SOLO_INFORMACION");
        String agentName = getParam(params, "agentName", "Equipo de Soporte FindU");
        String resolutionDetail = getParam(params, "resolutionDetail", "Tu solicitud fue atendida y resuelta con éxito por nuestro equipo de atención.");

        return "<!DOCTYPE html>\n" +
                "<html lang=\"es\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "  <title>Ticket Resuelto - FindU Soporte</title>\n" +
                "</head>\n" +
                "<body style=\"margin: 0; padding: 0; background-color: #f1f5f9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #1e293b;\">\n" +
                "  <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"background-color: #f1f5f9; padding: 32px 16px;\">\n" +
                "    <tr>\n" +
                "      <td align=\"center\">\n" +
                "        <table width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" border=\"0\" style=\"max-width: 600px; background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.05); border: 1px solid #e2e8f0;\">\n" +
                "          <tr>\n" +
                "            <td style=\"background: linear-gradient(135deg, #0284c7 0%, #0369a1 100%); padding: 32px; text-align: left;\">\n" +
                "              <h1 style=\"color: #ffffff; font-size: 24px; font-weight: 800; margin: 0; letter-spacing: -0.5px;\">Find<span style=\"color: #38bdf8;\">U</span> Soporte</h1>\n" +
                "              <div style=\"color: #e0f2fe; font-size: 13px; font-weight: 500; margin-top: 4px; text-transform: uppercase; letter-spacing: 1px;\">Atención al Usuario & Proveedor</div>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "          <tr>\n" +
                "            <td style=\"padding: 32px;\">\n" +
                "              <div style=\"display: inline-block; background-color: #ecfdf5; color: #047857; font-size: 12px; font-weight: 700; padding: 6px 14px; border-radius: 9999px; text-transform: uppercase; letter-spacing: 0.5px; border: 1px solid #a7f3d0; margin-bottom: 16px;\">&#10003; Caso Resuelto</div>\n" +
                "              <h2 style=\"font-size: 22px; font-weight: 700; color: #0f172a; margin: 0 0 12px 0;\">Tu ticket #" + ticketCode + " ha sido resuelto</h2>\n" +
                "              <p style=\"font-size: 15px; color: #334155; line-height: 1.6; margin-bottom: 24px;\">Hola <strong>" + userName + "</strong>,<br>Te informamos que nuestro equipo de soporte ha completado la revisión y atención de tu caso.</p>\n" +
                "              <div style=\"background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 12px; padding: 20px; margin-bottom: 24px;\">\n" +
                "                <table width=\"100%\" cellpadding=\"6\" cellspacing=\"0\" border=\"0\" style=\"font-size: 14px;\">\n" +
                "                  <tr>\n" +
                "                    <td style=\"color: #64748b; font-weight: 500;\">Número de Ticket:</td>\n" +
                "                    <td style=\"color: #0f172a; font-weight: 700; text-align: right;\">#" + ticketCode + "</td>\n" +
                "                  </tr>\n" +
                "                  <tr>\n" +
                "                    <td style=\"color: #64748b; font-weight: 500;\">Asunto:</td>\n" +
                "                    <td style=\"color: #0f172a; font-weight: 600; text-align: right;\">" + subject + "</td>\n" +
                "                  </tr>\n" +
                "                  <tr>\n" +
                "                    <td style=\"color: #64748b; font-weight: 500;\">Acción tomada:</td>\n" +
                "                    <td style=\"color: #0f172a; font-weight: 600; text-align: right;\">" + actionType + "</td>\n" +
                "                  </tr>\n" +
                "                  <tr>\n" +
                "                    <td style=\"color: #64748b; font-weight: 500;\">Atendido por:</td>\n" +
                "                    <td style=\"color: #0f172a; font-weight: 600; text-align: right;\">" + agentName + "</td>\n" +
                "                  </tr>\n" +
                "                </table>\n" +
                "              </div>\n" +
                "              <div style=\"background-color: #f0fdf4; border-left: 4px solid #10b981; border-radius: 8px; padding: 18px 20px; margin-bottom: 24px;\">\n" +
                "                <div style=\"font-size: 13px; font-weight: 700; color: #065f46; margin: 0 0 6px 0; text-transform: uppercase; letter-spacing: 0.5px;\">Detalle de la resolución</div>\n" +
                "                <p style=\"font-size: 15px; color: #166534; line-height: 1.6; margin: 0;\">" + resolutionDetail + "</p>\n" +
                "              </div>\n" +
                "              <div style=\"background-color: #eff6ff; border-radius: 10px; padding: 16px 20px; margin-bottom: 28px; border: 1px solid #dbeafe;\">\n" +
                "                <p style=\"font-size: 13px; color: #1e40af; line-height: 1.5; margin: 0;\">\n" +
                "                  <strong>¿Tu problema aún no ha sido solucionado por completo?</strong><br>\n" +
                "                  Puedes responder desde el chat de soporte o solicitar la reapertura de este ticket a través de tu aplicación móvil FindU.\n" +
                "                </p>\n" +
                "              </div>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "          <tr>\n" +
                "            <td style=\"background-color: #f8fafc; border-top: 1px solid #e2e8f0; padding: 24px 32px; text-align: center;\">\n" +
                "              <p style=\"font-size: 12px; color: #94a3b8; line-height: 1.5; margin: 0;\">&copy; FindU Technologies. Todos los derechos reservados.<br>Este es un mensaje automático del centro de ayuda y soporte de FindU.</p>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "        </table>\n" +
                "      </td>\n" +
                "    </tr>\n" +
                "  </table>\n" +
                "</body>\n" +
                "</html>";
    }

    private String getParam(Map<String, String> params, String key, String defaultValue) {
        if (params == null || !params.containsKey(key)) {
            return defaultValue;
        }
        String val = params.get(key);
        return (val != null && !val.isBlank()) ? val : defaultValue;
    }
}
