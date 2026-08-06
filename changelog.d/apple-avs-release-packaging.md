### Fixed
- Apple Maven KMP releases now include the AVS calling implementation and publish
  `com.wire:avs-kmp` as an iOS dependency instead of compiling the no-op Apple bridge.

### Migration
Consumers that require iOS calling must upgrade to an AVS-enabled Kalium release and
relink their final iOS framework or application binary.

### Compatibility
ABI: unchanged.
Source: unchanged.
Behavior: iOS calling is enabled in published Apple variants.
