/// Información fiscal para comprobantes AFIP
class FiscalInfo {
  final String razonSocial;
  final String cuit;
  final String direccion;
  final String localidad;
  final String numeroInscripcionIIBB;
  final String responsable;
  final String inicioActividades;
  final String fecha;
  final String numeroT;
  final String puntoVenta;
  final bool consumidorFinal;
  final String? regimenFiscal;
  final double? ivaContenido;
  final double? otrosImpuestosNacionales;
  final String? cae;
  final String? fechaVencimiento;
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

  factory FiscalInfo.fromMap(Map<String, dynamic> map) {
    return FiscalInfo(
      razonSocial: map['razonSocial'] as String? ?? '',
      cuit: map['cuit'] as String? ?? '',
      direccion: map['direccion'] as String? ?? '',
      localidad: map['localidad'] as String? ?? '',
      numeroInscripcionIIBB: map['numeroInscripcionIIBB'] as String? ?? '',
      responsable: map['responsable'] as String? ?? '',
      inicioActividades: map['inicioActividades'] as String? ?? '',
      fecha: map['fecha'] as String? ?? '',
      numeroT: map['numeroT'] as String? ?? '',
      puntoVenta: map['puntoVenta'] as String? ?? '',
      consumidorFinal: map['consumidorFinal'] as bool? ?? true,
      regimenFiscal: map['regimenFiscal'] as String?,
      ivaContenido: (map['ivaContenido'] as num?)?.toDouble(),
      otrosImpuestosNacionales:
          (map['otrosImpuestosNacionales'] as num?)?.toDouble(),
      cae: map['cae'] as String?,
      fechaVencimiento: map['fechaVencimiento'] as String?,
      qrCodeData: map['qrCodeData'] as String?,
    );
  }
}
