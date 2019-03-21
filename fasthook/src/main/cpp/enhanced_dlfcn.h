//
// Created on 2019/3/20.
//

#include <stdlib.h>
#include <string.h>
#include <unistd.h>

int enhanced_dlclose(void *handle);
void *enhanced_dlopen(const char *libpath, int flags);
void *enhanced_dlsym(void *handle, const char *name);
