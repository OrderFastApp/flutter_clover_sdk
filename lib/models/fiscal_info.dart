/// Modelo que representa la información fiscal de un ticket
class FiscalInfo {
  /// Razón social de la empresa
  final String razonSocial;

  /// CUIT de la empresa
  final String cuit;

  /// Dirección de la empresa
  final String direccion;

  /// Localidad de la empresa
  final String localidad;

  /// Número de inscripción en IIBB
  final String numeroInscripcionIIBB;

  /// Responsable (ej: Monotributo, Responsable Inscripto)
  final String responsable;

  /// Fecha de inicio de actividades
  final String inicioActividades;

  /// Fecha de la transacción
  final String fecha;

  /// Número de transacción
  final String numeroT;

  /// Punto de venta
  final String puntoVenta;

  /// Si es consumidor final
  final bool consumidorFinal;

  /// Régimen fiscal aplicable
  final String? regimenFiscal;

  /// IVA contenido en el total
  final double? ivaContenido;

  /// Otros impuestos nacionales indirectos
  final double? otrosImpuestosNacionales;

  /// Código de Autorización Electrónico (CAE)
  final String? cae;

  /// Fecha de vencimiento del CAE
  final String? fechaVencimiento;

  /// Datos del código QR (si aplica)
  final String? qrCodeData;

  FiscalInfo({
    required this.razonSocial,
    required this.cuit,
    required this.direccion,
    required this.localidad,
    required this.numeroInscripcionIIBB,
    required this.responsable,
    required this.inicioActividades,
    required this.fecha,
    required this.numeroT,
    required this.puntoVenta,
    this.consumidorFinal = true,
    this.regimenFiscal,
    this.ivaContenido,
    this.otrosImpuestosNacionales,
    this.cae,
    this.fechaVencimiento,
    this.qrCodeData,
  });

  /// Convierte el modelo a un Map para enviarlo al plugin nativo
  Map<String, dynamic> toMap() {
    return {
      'razonSocial': razonSocial,
      'cuit': cuit,
      'direccion': direccion,
      'localidad': localidad,
      'numeroInscripcionIIBB': numeroInscripcionIIBB,
      'responsable': responsable,
      'inicioActividades': inicioActividades,
      'fecha': fecha,
      'numeroT': numeroT,
      'puntoVenta': puntoVenta,
      'consumidorFinal': consumidorFinal,
      if (regimenFiscal != null) 'regimenFiscal': regimenFiscal,
      if (ivaContenido != null) 'ivaContenido': ivaContenido,
      if (otrosImpuestosNacionales != null)
        'otrosImpuestosNacionales': otrosImpuestosNacionales,
      if (cae != null) 'cae': cae,
      if (fechaVencimiento != null) 'fechaVencimiento': fechaVencimiento,
      if (qrCodeData != null) 'qrCodeData': qrCodeData,
    };
  }

  /// Crea un FiscalInfo desde un Map
  factory FiscalInfo.fromMap(Map<String, dynamic> map) {
    return FiscalInfo(
      razonSocial: map['razonSocial'] as String,
      cuit: map['cuit'] as String,
      direccion: map['direccion'] as String,
      localidad: map['localidad'] as String,
      numeroInscripcionIIBB: map['numeroInscripcionIIBB'] as String,
      responsable: map['responsable'] as String,
      inicioActividades: map['inicioActividades'] as String,
      fecha: map['fecha'] as String,
      numeroT: map['numeroT'] as String,
      puntoVenta: map['puntoVenta'] as String,
      consumidorFinal: map['consumidorFinal'] as bool? ?? true,
      regimenFiscal: map['regimenFiscal'] as String?,
      ivaContenido: map['ivaContenido'] != null
          ? (map['ivaContenido'] as num).toDouble()
          : null,
      otrosImpuestosNacionales: map['otrosImpuestosNacionales'] != null
          ? (map['otrosImpuestosNacionales'] as num).toDouble()
          : null,
      cae: map['cae'] as String?,
      fechaVencimiento: map['fechaVencimiento'] as String?,
      qrCodeData: map['qrCodeData'] as String?,
    );
  }
}
