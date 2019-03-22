#include <android/log.h>

#define LOG_TAG "FastHookManager"
#define LOG_DEBUG 1

#if LOG_DEBUG
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)
#else
#define LOGI(...)
#endif

uint32_t kPointerSize32 = 4;
uint32_t kPointerSize64 = 8;

uint32_t kHiddenApiPolicyOffset = 0;

uint32_t kAccNative = 0x0100;
uint32_t kAccCompileDontBother = 0;

uint32_t kArtMethodAccessFlagsOffset = 4;
uint32_t kArtMethodHotnessCountOffset = 0;
uint32_t kArtMethodProfilingOffset = 0;
uint32_t kArtMethodInterpreterEntryOffset = 0;
uint32_t kArtMethodQuickCodeOffset = 0;
uint32_t kProfilingCompileStateOffset = 0;
uint32_t kProfilingSavedEntryPointOffset = 0;

uint32_t kHotMethodThreshold = 0;
uint32_t kHotMethodMaxCount = 0;

uint32_t kCodeSizeMask = ~0x80000000;

const char* const kClassName = "pers/turing/technician/fasthook/FastHookManager";
const char* const kHookRecordClassName = "pers/turing/technician/fasthook/FastHookManager$HookRecord";

enum Version {
	kAndroidL = 21,
	kAndroidLMR1,
	kAndroidM,
	kAndroidN,
	kAndroidNMR1,
	kAndroidO,
	kAndroidOMR1,
	kAndroidP
};

enum TrampolineType {
	kJumpTrampoline,
	kQuickHookTrampoline,
	kQuickOriginalTrampoline,
	kQuickTargetTrampoline,
	kHookTrampoline,
	kTargetTrampoline
};

enum JitState {
	kNone,
	kCompile,
	kCompiling,
	kCompilingOrFailed
};

static struct {
	jfieldID jump_trampoline_;
	jfieldID quick_hook_trampoline_;
	jfieldID quick_original_trampoline_;
	jfieldID quick_target_trampoline_;
	jfieldID hook_trampoline_;
	jfieldID target_trampoline_;
} kHookRecordClassInfo;

struct SigactionInfo {
    void *addr;
    int len;
};

#if defined(__aarch64__)
# define __get_tls() ({ void** __val; __asm__("mrs %0, tpidr_el0" : "=r"(__val)); __val; })
#elif defined(__arm__)
# define __get_tls() ({ void** __val; __asm__("mrc p15, 0, %0, c13, c0, 3" : "=r"(__val)); __val; })
#endif

uint32_t kTLSSlotArtThreadSelf = 0;

struct SigactionInfo *sigaction_info_ = NULL;
struct sigaction *default_handler_ = NULL;
struct sigaction *current_handler_ = NULL;

JavaVM *jvm_ = NULL;
void *runtime_ = NULL;

void* (*jit_load_)(bool*) = NULL;
void* jit_compiler_handle_ = NULL;
bool (*jit_compile_method_)(void*, void*, void*, bool) = NULL;
void** art_jit_compiler_handle_ = NULL;
void *art_quick_to_interpreter_bridge_ = NULL;

uint32_t pointer_size_ = 0;

#if defined(__arm__)

//DF F8 00 F0 ; ldr pc, [pc, #0]
//00 00 00 00 ; targetAddress
unsigned char jump_trampoline_[] = {
		0xdf, 0xf8, 0x00, 0xf0,
		0x00, 0x00, 0x00, 0x00
};

//DF F8 1C C0 ; ldr ip, [pc, #28] 1f
//60 45       ; cmp r0, ip
//00 bf       ; nop
//40 F0 06 80 ; bne.w #12
//05 48       ; ldr r0, [pc, #20] 2f
//00 bf       ; nop
//DF F8 14 F0 ; ldr ip, [pc ,#20] 3f
//E7 46       ; mov pc, ip
//00 bf       ; nop
//DF F8 10 C0 ; ldr ip, [pc, #16] 4f
//E7 46       ; mov pc, ip
//00 bf       ; nop
//00 00 00 00 ; 1f:target method
//00 00 00 00 ; 2f:hook method
//00 00 00 00 ; 3f:hook method entry point
//00 00 00 00 ; 4f:other entry point
unsigned char quick_hook_trampoline_[] = {
		0xdf, 0xf8, 0x1c, 0xc0,
		0x60, 0x45,
		0x00, 0xbf,
		0x40, 0xf0, 0x06, 0x80,
		0x05, 0x48,
		0x00, 0xbf,
		0xdf, 0xf8, 0x14, 0xf0,
		0xe7, 0x46,
		0x00, 0xbf,
		0xdf, 0xf8, 0x10, 0xc0,
		0xe7, 0x46,
		0x00, 0xbf,
		0x00, 0x00, 0x00, 0x00,
		0x00, 0x00, 0x00, 0x00,
		0x00, 0x00, 0x00, 0x00,
		0x00, 0x00, 0x00, 0x00
};

//04 48       ; ldr r0, [pc, #16] 1f
//00 bf       ; nop
//00 00 00 00 ; original prologue
//00 00 00 00 ; original prologue
//00 bf 00 bf ; original prologue
//DF F8 04 F0 ; ldr pc, [pc, #4]
//00 00 00 00 ; original method
//00 00 00 00 ; original code point
unsigned char quick_target_trampoline_[] = {
		0x04, 0x48,
		0x00, 0xbf,
		0x00, 0x00, 0x00, 0x00,
		0x00, 0x00, 0x00, 0x00,
		0x00, 0xbf, 0x00, 0xbf,
		0xdf, 0xf8, 0x04, 0xf0,
		0x00, 0x00, 0x00, 0x00,
		0x00, 0x00, 0x00, 0x00
};

//01 48       ; ldr r0, [pc, #4]
//00 bf       ; nop
//D0 F8 1C F0 ; ldr pc, [r0, #28]
//00 00 00 00 ; hook method
unsigned char hook_trampoline_[] = {
		0x01, 0x48,
		0x00, 0xbf,
		0xd0, 0xf8, 0x1c, 0xf0,
		0x00, 0x00, 0x00, 0x00
};


//01 48       ; ldr r0, [pc, #4]
//00 bf       ; nop
//DF F8 04 F0 ; ldr pc, [pc, #4]
//00 00 00 00 ; original method
//00 00 00 00 ; original method entry point
unsigned char target_trampoline_[] = {
		0x01, 0x48,
		0x00, 0xbf,
		0xdf, 0xf8, 0x04, 0xf0,
		0x00, 0x00, 0x00, 0x00,
		0x00, 0x00, 0x00, 0x00
};

#elif defined(__aarch64__)

//50 00 00 58 ; ldr x16, #8 1f
//00 02 1F D6 ; br x16
//00 00 00 00 00 00 00 00 ; 1f:hook method entry point
unsigned char jump_trampoline_[] = {
        0x50, 0x00, 0x00, 0x58,
        0x00, 0x02, 0x1f, 0xd6,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
};

//11 01 00 58 ; ldr x17, #32 1f
//1F 00 11 EB ; cmp x0, x17
//81 00 00 54 ; bne #16
//E0 00 00 58 ; ldr x0, #28 2f
//11 01 00 58 ; ldr x17, #32 3f
//20 02 1F D6 ; br x17
//11 01 00 58 ; ldr x17, #32 4f
//20 02 1F D6 ; br x17
//00 00 00 00 00 00 00 00 ; 1f:target method
//00 00 00 00 00 00 00 00 ; 2f:hook method
//00 00 00 00 00 00 00 00 ; 3f:hook method entry point
//00 00 00 00 00 00 00 00 ; 4f:other entry point
unsigned char quick_hook_trampoline_[] = {
        0x11, 0x01, 0x00, 0x58,
        0x1f, 0x00, 0x11, 0xeb,
        0x81, 0x00, 0x00, 0x54,
        0xe0, 0x00, 0x00, 0x58,
        0x11, 0x01, 0x00, 0x58,
        0x20, 0x02, 0x1f, 0xd6,
        0x11, 0x01, 0x00, 0x58,
        0x20, 0x02, 0x1f, 0xd6,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
};

//E0 00 00 58 ; ldr x0, #28 1f
//00 00 00 00 ; original prologue
//00 00 00 00 ; original prologue
//00 00 00 00 ; original prologue
//00 00 00 00 ; original prologue
//91 00 00 58 ; ldr x17, #16 2f
//20 02 1F D6 ; br x17
//00 00 00 00 00 00 00 00 ; 1f:original method
//00 00 00 00 00 00 00 00 ; 2f:original code point
unsigned char quick_target_trampoline_[] = {
        0xe0, 0x00, 0x00, 0x58,
        0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00,
        0x91, 0x00, 0x00, 0x58,
        0x20, 0x02, 0x1f, 0xd6,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
};

//60 00 00 58 ; ldr x0, #12 1f
//10 14 40 F9 ; ldr x16, [x0, #40]
//00 02 1F D6 ; br x16
//00 00 00 00 00 00 00 00 ; 1f:hook method
unsigned char hook_trampoline_[] = {
        0x60, 0x00, 0x00, 0x58,
        0x10, 0x14, 0x40, 0xf9,
        0x00, 0x02, 0x1f, 0xd6,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
};

//60 00 00 58 ; ldr x0, #12 1f
//90 00 00 58 ; ldr x16, #16 2f
//00 02 1f d6 ; br x16
//00 00 00 00 00 00 00 00 ; 1f:original method
//00 00 00 00 00 00 00 00 ; 1f:original method entry point
unsigned char target_trampoline_[] = {
        0x60, 0x00, 0x00, 0x58,
        0x90, 0x00, 0x00, 0x58,
        0x00, 0x02, 0x1f, 0xd6,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
};
#endif

static inline long ReadPointer(void *pointer) {
	return *((long *)pointer);
}

static inline uint32_t ReadInt32(void *pointer) {
	return *((uint32_t *)pointer);
}

static inline uint16_t ReadInt16(void *pointer) {
	return *((uint16_t *)pointer);
}

static inline uint8_t ReadInt8(void *pointer) {
    return *((uint8_t *)pointer);
}

static inline uint32_t RoundUp(uint32_t size, uint32_t ptr_size) {
	return (size + ptr_size - 1) - ((size + ptr_size - 1) & (ptr_size - 1));
}

static inline bool IsLittleEnd() {
	bool ret = true;

	int i = 1;
	if(!(*(char*)&i == 1)) {
		ret = false;
	}

	return ret;
}

static inline bool IsThumb32(uint16_t inst, bool little_end) {
	if(little_end) {
		return ((inst & 0xe000) == 0xe000 && (inst & 0x1800) != 0x0000);
	}
	return ((inst & 0x00e0) == 0x00e0 && (inst & 0x0018) != 0x0000);
}

static inline bool HasArm64PcRelatedInst(uint32_t inst) {
	uint32_t mask_b = 0xfc000000;
	uint32_t op_b = 0x14000000;
	uint32_t op_bl = 0x94000000;

	uint32_t mask_bc = 0xff000010;
	uint32_t op_bc = 0x54000000;

	uint32_t mask_cb = 0x7f000000;
	uint32_t op_cbz = 0x34000000;
	uint32_t op_cbnz = 0x35000000;

	uint32_t mask_tb = 0x7f000000;
	uint32_t op_tbz = 0x36000000;
	uint32_t op_tbnz = 0x37000000;

	uint32_t mask_ldr = 0xbf000000;
	uint32_t op_ldr = 0x18000000;

	uint32_t mask_adr = 0x9f000000;
	uint32_t op_adr = 0x10000000;
	uint32_t op_adrp = 0x90000000;

	if((inst & mask_b) == op_b || (inst & mask_b) == op_bl)
		return true;

	if((inst & mask_bc) == op_bc)
		return true;

	if((inst & mask_cb) == op_cbz || (inst & mask_cb) == op_cbnz)
		return true;

	if((inst & mask_tb) == op_tbz || (inst & mask_tb) == op_tbnz)
		return true;

	if((inst & mask_ldr) == op_ldr)
		return true;

	if((inst & mask_adr) == op_adr || (inst & mask_adr) == op_adrp)
		return true;

	return false;
}

static inline bool HasThumb32PcRelatedInst(uint32_t inst) {
	uint32_t mask_b = 0xf800d000;
	uint32_t op_blx = 0xf000c000;
	uint32_t op_bl = 0xf000d000;
	uint32_t op_b1 = 0xf0008000;
	uint32_t op_b2 = 0xf0009000;

	uint32_t mask_adr = 0xfbff8000;
	uint32_t op_adr1 = 0xf2af0000;
	uint32_t op_adr2 = 0xf20f0000;

	uint32_t mask_ldr = 0xff7f0000;
	uint32_t op_ldr = 0xf85f0000;

	uint32_t mask_tb = 0xffff00f0;
	uint32_t op_tbb = 0xe8df0000;
	uint32_t op_tbh = 0xe8df0010;

	if((inst & mask_b) == op_blx || (inst & mask_b) == op_bl || (inst & mask_b) == op_b1 || (inst & mask_b) == op_b2)
		return true;

	if((inst & mask_adr) == op_adr1 || (inst & mask_adr) == op_adr2)
		return true;

	if((inst & mask_ldr) == op_ldr)
		return true;

	if((inst & mask_tb) == op_tbb || (inst & mask_tb) == op_tbh)
		return true;

	return false;
}

static inline bool HasThumb16PcRelatedInst(uint16_t inst) {
	uint16_t mask_b1 = 0xf000;
	uint16_t op_b1 = 0xd000;

	uint16_t mask_b2_adr_ldr = 0xf800;
	uint16_t op_b2 = 0xe000;
	uint16_t op_adr = 0xa000;
	uint16_t op_ldr = 0x4800;

	uint16_t mask_bx = 0xfff8;
	uint16_t op_bx = 0x4778;

	uint16_t mask_add_mov = 0xff78;
	uint16_t op_add = 0x4478;
	uint16_t op_mov = 0x4678;

	uint16_t mask_cb = 0xf500;
	uint16_t op_cb = 0xb100;

	if((inst & mask_b1) == op_b1)
		return true;

	if((inst * mask_b2_adr_ldr) == op_b2 || (inst * mask_b2_adr_ldr) == op_adr || (inst * mask_b2_adr_ldr) == op_ldr)
		return true;

	if((inst & mask_bx) == op_bx)
		return true;

	if((inst & mask_add_mov) == op_add || (inst & mask_add_mov) == op_mov)
		return true;

	if((inst & mask_cb) == op_cb)
		return true;

	return false;
}