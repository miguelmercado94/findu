/**
 * Puertos de salida (output ports): interfaces que definen qué necesita la aplicación
 * del exterior (persistencia, APIs, eventos). Las implementaciones (adapters) viven
 * en infrastructure.adapter.
 *
 * <ul>
 *   <li>persistence – persistencia en BD (implementado por infrastructure.adapter.persistence)</li>
 *   <li>externalapi – (futuro) llamadas a APIs externas</li>
 *   <li>events – (futuro) publicación/consumo desde bucket de eventos</li>
 * </ul>
 */
package com.findu.security.application.port.output;
