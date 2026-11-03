package com.findu.core.domain.enums;

import java.util.List;

public enum CategoriaCancelacionProveedor {

    CATEGORIA_1_CLIENTE(
            "Motivos relacionados con el cliente",
            "Tu solicitud de cancelación será enviada a soporte para su validación. El servicio no se cancelará hasta que soporte revise y apruebe tu solicitud.",
            List.of(
                    "El cliente solicita cancelar",
                    "El cliente modifica las condiciones del servicio",
                    "El cliente cambia la ubicación",
                    "El cliente no responde",
                    "El cliente no proporciona la información necesaria",
                    "Las condiciones reales no corresponden con lo acordado"
            )
    ),
    CATEGORIA_2_FUERZA_MAYOR(
            "Fuerza mayor o emergencias",
            "Por tratarse de una situación de fuerza mayor, debes adjuntar un soporte que permita validar tu solicitud. Puedes cargarlo ahora o hacerlo posteriormente desde tu ticket de soporte. Si después de 3 días no presentas un soporte válido, recibirás una amonestación grave.",
            List.of(
                    "Emergencia médica o enfermedad",
                    "Accidente",
                    "Emergencia familiar",
                    "Fallecimiento de un familiar",
                    "Desastres naturales",
                    "Situaciones de orden público",
                    "Riesgos para la seguridad e integridad personal"
            )
    ),
    CATEGORIA_3_PERSONAL_OPERATIVO(
            "Motivos personales u operativos",
            "Esta cancelación se registrará como atribuible al proveedor y será evaluada dentro de tu historial de los últimos 30 días.",
            List.of(
                    "Indisponibilidad de horario",
                    "Compromisos personales",
                    "Problemas de transporte",
                    "Problemas con herramientas o materiales",
                    "Falta de recursos necesarios",
                    "Error al aceptar o confirmar el servicio"
            )
    ),
    CATEGORIA_4_OTROS(
            "Otros motivos",
            "Tu solicitud será evaluada por soporte. En caso de determinarse injustificada, se contabilizará dentro de tu historial de los últimos 30 días.",
            List.of(
                    "Otro motivo (especificar en descripción)"
            )
    );

    private final String titulo;
    private final String advertenciaBase;
    private final List<String> motivosPermitidos;

    CategoriaCancelacionProveedor(String titulo, String advertenciaBase, List<String> motivosPermitidos) {
        this.titulo = titulo;
        this.advertenciaBase = advertenciaBase;
        this.motivosPermitidos = motivosPermitidos;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getAdvertenciaBase() {
        return advertenciaBase;
    }

    public List<String> getMotivosPermitidos() {
        return motivosPermitidos;
    }
}

