/// Modelo que representa la respuesta de una operación de impresión
class PrintTicketResponse {
  /// Indica si la impresión fue exitosa
  final bool success;

  /// Mensaje de éxito o información adicional
  final String? message;

  /// Mensaje de error si la impresión falló
  final String? error;

  PrintTicketResponse({
    required this.success,
    this.message,
    this.error,
  });

  /// Crea un PrintTicketResponse desde un Map
  factory PrintTicketResponse.fromMap(Map<String, dynamic> map) {
    return PrintTicketResponse(
      success: map['success'] as bool,
      message: map['message'] as String?,
      error: map['error'] as String?,
    );
  }

  /// Convierte el modelo a un Map
  Map<String, dynamic> toMap() {
    return {
      'success': success,
      if (message != null) 'message': message,
      if (error != null) 'error': error,
    };
  }
}
