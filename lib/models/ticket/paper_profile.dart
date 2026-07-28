/// Perfil de papel para la impresión de tickets
enum PaperProfile {
  /// Perfil 80mm (576 px / ~32-38 caracteres) - Clover Station Duo / Impresoras de Cocina
  mm80,

  /// Perfil 55mm (384 px / ~22-26 caracteres) - Clover Flex / Clover Mini
  mm55,

  /// Detección automática según el dispositivo físico Clover
  auto,
}

extension PaperProfileExtension on PaperProfile {
  String get name {
    switch (this) {
      case PaperProfile.mm80:
        return 'mm80';
      case PaperProfile.mm55:
        return 'mm55';
      case PaperProfile.auto:
        return 'auto';
    }
  }

  static PaperProfile fromString(String? value) {
    switch (value?.toLowerCase()) {
      case 'mm80':
      case '80mm':
      case '80':
        return PaperProfile.mm80;
      case 'mm55':
      case '55mm':
      case '55':
        return PaperProfile.mm55;
      default:
        return PaperProfile.auto;
    }
  }
}
