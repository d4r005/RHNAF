package com.example.rhnaf.routes

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

/**
 * Paginas publicas (sin login) de Politica de Privacidad y Condiciones del
 * Servicio. Google las exige en la pantalla de consentimiento de OAuth:
 *   https://<host>/privacidad
 *   https://<host>/condiciones
 */
private const val EMPRESA = "Shelser (North America Flooring)"
private const val APP = "RHNAF Industrial"
private const val CONTACTO = "d.trujillo@brancoindustries.com"
private const val FECHA = "14 de octubre de 2026"

private fun pagina(titulo: String, cuerpo: String): String = """<!DOCTYPE html>
<html lang="es"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>$titulo | $APP</title>
<style>
body{font-family:system-ui,-apple-system,Segoe UI,Roboto,Arial,sans-serif;background:#f8fafc;color:#0f172a;margin:0;line-height:1.6}
main{max-width:820px;margin:0 auto;padding:32px 20px 64px}
h1{font-size:28px;margin:0 0 4px}h2{font-size:18px;margin:28px 0 6px;color:#1e293b}
.meta{color:#64748b;font-size:14px;margin-bottom:24px}
a{color:#2563eb}li{margin:4px 0}
nav{margin-top:40px;font-size:14px;border-top:1px solid #e2e8f0;padding-top:12px}
</style></head><body><main>
<h1>$titulo</h1><div class="meta">$APP · $EMPRESA · Última actualización: $FECHA</div>
$cuerpo
<nav><a href="/privacidad">Política de Privacidad</a> · <a href="/condiciones">Condiciones del Servicio</a> · <a href="/">Ir a la aplicación</a></nav>
</main></body></html>"""

private val PRIVACIDAD = """
<p>Esta Política de Privacidad describe cómo <b>$EMPRESA</b> ("la Empresa", "nosotros") recopila, usa y protege la información en <b>$APP</b> ("la Aplicación"), el sistema interno de recursos humanos, seguridad e higiene (EHS), calidad (QIS) y operación de planta.</p>

<h2>1. Quién puede usar la Aplicación</h2>
<p>La Aplicación es de uso interno y exclusivo para el personal autorizado de la Empresa. El acceso se controla con usuarios y roles (administración, recursos humanos, seguridad, compras, almacén, entre otros).</p>

<h2>2. Información que recopilamos</h2>
<ul>
<li><b>Datos de personal:</b> nombre, número de empleado, puesto, departamento, fecha de ingreso, RFC, CURP, NSS, datos de contacto y, cuando aplica, fotografía.</li>
<li><b>Asistencia:</b> registros de entrada y salida generados por los lectores biométricos/faciales de la planta.</li>
<li><b>Nómina:</b> sueldo, salario base de cotización, percepciones, deducciones y pre-nómina.</li>
<li><b>EHS y salud ocupacional:</b> capacitaciones, equipo de protección, incidentes, inspecciones y evidencias. Los exámenes médicos son de acceso restringido al área de Seguridad.</li>
<li><b>Calidad y control de documentos:</b> documentos controlados, versiones y bitácora de cambios.</li>
<li><b>Lockers:</b> asignación de casilleros, entrega de candado, llaves copia y bitácora histórica de titulares.</li>
<li><b>Datos de acceso:</b> correo electrónico del usuario, rol y registros técnicos de uso para seguridad y auditoría.</li>
</ul>

<h2>3. Para qué usamos la información</h2>
<ul>
<li>Administrar al personal, la asistencia y el cálculo de pre-nómina.</li>
<li>Cumplir obligaciones laborales, fiscales, de seguridad social y de seguridad e higiene.</li>
<li>Mantener la trazabilidad del sistema de gestión de calidad.</li>
<li>Resguardar evidencias y documentos de la operación.</li>
<li>Proteger las instalaciones y a las personas.</li>
</ul>
<p>No vendemos ni compartimos datos personales con terceros con fines comerciales ni publicitarios.</p>

<h2>4. Integración con Google Drive</h2>
<p>Para resguardar documentos y evidencias, la Aplicación puede conectarse a una cuenta de <b>Google Drive</b> de la Empresa mediante OAuth 2.0 con el alcance restringido <code>drive.file</code>. Esto significa que la Aplicación solo puede ver y administrar los archivos <b>que ella misma crea o sube</b>; no accede a otros archivos de la cuenta.</p>
<ul>
<li>Usamos este acceso únicamente para subir, organizar y descargar documentos de la Aplicación (evidencias EHS, documentos de calidad, listados de lockers y similares).</li>
<li>El token de autorización se almacena de forma segura como secreto del servidor y no se comparte con terceros.</li>
<li>El uso de la información obtenida de las APIs de Google cumple con la <a href="https://developers.google.com/terms/api-services-user-data-policy" target="_blank" rel="noopener">Política de Datos de Usuario de los Servicios de API de Google</a>, incluidos sus requisitos de Uso Limitado.</li>
<li>Puedes revocar el acceso en cualquier momento desde <a href="https://myaccount.google.com/permissions" target="_blank" rel="noopener">tu cuenta de Google</a>.</li>
</ul>

<h2>5. Servicios de terceros</h2>
<p>La Aplicación se aloja en Hugging Face Spaces y utiliza una base de datos administrada (Supabase/PostgreSQL) y el código se mantiene en GitHub. Estos proveedores procesan datos únicamente para prestar el servicio de infraestructura.</p>

<h2>6. Conservación</h2>
<p>Conservamos la información durante el tiempo necesario para cumplir las finalidades descritas y las obligaciones legales aplicables. Algunas bitácoras (control de documentos, lockers) son inmutables por requisitos de trazabilidad.</p>

<h2>7. Seguridad</h2>
<p>Aplicamos controles de acceso por roles, autenticación, comunicaciones cifradas (HTTPS) y resguardo de credenciales como secretos. Ningún sistema es infalible, pero trabajamos para reducir riesgos de acceso no autorizado, pérdida o alteración.</p>

<h2>8. Derechos de las personas titulares (ARCO)</h2>
<p>Conforme a la Ley Federal de Protección de Datos Personales en Posesión de los Particulares (México), las personas titulares pueden ejercer sus derechos de <b>acceso, rectificación, cancelación y oposición</b>, así como revocar su consentimiento, escribiendo a <a href="mailto:$CONTACTO">$CONTACTO</a>. Responderemos en los plazos legales.</p>

<h2>9. Cambios a esta política</h2>
<p>Podemos actualizar esta política. La versión vigente siempre estará disponible en esta dirección, con su fecha de actualización.</p>

<h2>10. Contacto</h2>
<p><b>$EMPRESA</b> · <a href="mailto:$CONTACTO">$CONTACTO</a></p>
"""

private val CONDICIONES = """
<p>Estas Condiciones del Servicio regulan el uso de <b>$APP</b> ("la Aplicación"), sistema interno de <b>$EMPRESA</b> ("la Empresa"). Al iniciar sesión o usar la Aplicación aceptas estas condiciones.</p>

<h2>1. Uso autorizado</h2>
<p>La Aplicación es de uso interno. Solo pueden usarla las personas a quienes la Empresa haya otorgado una cuenta. Cada usuario es responsable de las acciones realizadas con su cuenta y de mantener la confidencialidad de sus credenciales.</p>

<h2>2. Roles y permisos</h2>
<p>El acceso a los módulos y la capacidad de consultar o modificar información dependen del rol asignado (por ejemplo: administración, recursos humanos, seguridad, almacén, compras). Está prohibido intentar acceder a información o funciones fuera de tu rol.</p>

<h2>3. Conductas no permitidas</h2>
<ul>
<li>Compartir cuentas o contraseñas.</li>
<li>Capturar datos falsos o manipular registros de asistencia, nómina, documentos o bitácoras.</li>
<li>Extraer, copiar o divulgar datos personales de compañeros fuera de los fines laborales.</li>
<li>Cargar archivos con virus, código malicioso o contenido ilícito.</li>
<li>Intentar vulnerar la seguridad, saturar o interferir con el servicio.</li>
</ul>

<h2>4. Información y documentos</h2>
<p>La información capturada en la Aplicación es propiedad de la Empresa. Los documentos controlados y las bitácoras (calidad, lockers) conservan su historial para trazabilidad y no se eliminan mediante el uso normal del sistema.</p>

<h2>5. Google Drive</h2>
<p>La Aplicación puede guardar archivos en una cuenta de Google Drive de la Empresa con el alcance <code>drive.file</code>. El uso de Google Drive está sujeto además a las <a href="https://policies.google.com/terms" target="_blank" rel="noopener">Condiciones del Servicio de Google</a>.</p>

<h2>6. Disponibilidad</h2>
<p>Hacemos un esfuerzo razonable por mantener la Aplicación disponible, pero puede haber interrupciones por mantenimiento, fallas de proveedores externos (hospedaje, base de datos, Google) o causas fuera de nuestro control. No garantizamos disponibilidad ininterrumpida.</p>

<h2>7. Cálculos informativos</h2>
<p>La pre-nómina y otros cálculos son <b>documentos informativos</b> de apoyo. Los importes definitivos de nómina están sujetos a validación por el área responsable.</p>

<h2>8. Limitación de responsabilidad</h2>
<p>En la medida permitida por la ley, la Empresa no será responsable por daños indirectos derivados del uso o la imposibilidad de uso de la Aplicación, ni por información capturada de forma incorrecta por los usuarios.</p>

<h2>9. Suspensión</h2>
<p>La Empresa puede suspender o cancelar cuentas por incumplimiento de estas condiciones, por terminación de la relación laboral o por razones de seguridad.</p>

<h2>10. Privacidad</h2>
<p>El tratamiento de datos personales se describe en nuestra <a href="/privacidad">Política de Privacidad</a>.</p>

<h2>11. Cambios y ley aplicable</h2>
<p>Podemos modificar estas condiciones; la versión vigente estará siempre en esta dirección. Se rigen por las leyes de los Estados Unidos Mexicanos, con jurisdicción en los tribunales competentes del domicilio de la Empresa.</p>

<h2>12. Contacto</h2>
<p><b>$EMPRESA</b> · <a href="mailto:$CONTACTO">$CONTACTO</a></p>
"""

fun Route.legalPagesRouting() {
    get("/privacidad") { call.respondText(pagina("Política de Privacidad", PRIVACIDAD), ContentType.Text.Html) }
    get("/condiciones") { call.respondText(pagina("Condiciones del Servicio", CONDICIONES), ContentType.Text.Html) }
}
