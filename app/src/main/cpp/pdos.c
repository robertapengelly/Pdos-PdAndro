
// The authors disclam copyright to this source code

#include <stdio.h>
#include <unistd.h>
#include <fcntl.h>
#include <stdlib.h>
#include <sys/mman.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <string.h>
#include <errno.h>

static int new_mode;
static int old_mode;
void printenv()
{
    char **env;
    env = environ;
    while (env[0]) {
        if (strstr(env[0], "PATH=")) {
            write(1, env[0], strlen(env[0]));
            write(1, "\n", 1);
        }
        env++;
    }
}

int run(char *cmd, char *arg1)
{
    pid_t pid;
    int in;
    int out;
    int status = -1;
    char *args[10];
    args[0] = cmd;
    args[1] = arg1;
    args[2] = NULL;

    fcntl(STDIN_FILENO, F_SETFL, old_mode);
    pid = fork();
    in = dup(0);
    out = dup(1);
    if (pid == 0) {;
        execv(args[0], args);
        write(STDOUT_FILENO, "!exe\n", 5);
        exit(-1);
    } else {
        waitpid(pid, &status, 0);
        dup2(in, 0);
        dup2(out, 1);
        fcntl(STDIN_FILENO, F_SETFL, new_mode);
   }

    return status;
}

#define BIN_SIZE 32 * 1024 * 1024
int main(int argc, char *argv[])
{
    int n = 0;
    FILE *fp = stdin;
    int fd = STDIN_FILENO;
    char cmd[1024];
    char b[1024];
    char *prompt = NULL;
#if 0 // amd64 in RAM execution test
/*    char assembly[] = {0x48, 0xc7, 0xc0, 0x10, 0x00, 0x00, 0x00, //          movq    $16, %rax
                    0xc3}; //                            retq
  */   // Arm32
    char assembly[] = {0x11, 0x00, 0xa0, 0xe3, //   mov     r0, #17
                        0x1e, 0xff, 0x2f, 0xe1}; //   bx lr

    char *bin;
    int ret;
    char *err;
    long pagesize = sysconf(_SC_PAGESIZE);
    long memsize = (BIN_SIZE + pagesize -1) / pagesize * pagesize;
    bin = (long)malloc(BIN_SIZE + pagesize - 1) / pagesize * pagesize;
    //bin = malloc(pagesize);
    memcpy(bin, assembly, sizeof(assembly));
    ret = mprotect(bin, memsize, PROT_EXEC|PROT_READ|PROT_WRITE);
    if (ret != 0) {
        err = strerror(errno);
        write(STDOUT_FILENO, err, strlen(err));

    }
    ret = ((int(*)())bin)();
    snprintf(cmd, 10, "R%ld", ret);
    sleep(1);
    write(STDOUT_FILENO, cmd, strlen(cmd));
#endif

    sleep(1);

    if (argc > 1) {
        write(STDOUT_FILENO, argv[1], strlen(argv[1]));
    }
    if (argc > 2) {
        write(STDOUT_FILENO, "\n\n", 2);
        write(STDOUT_FILENO, argv[2], strlen(argv[2]));
    }
    
    write(STDOUT_FILENO, "\n", 1);
    sleep(2);
    snprintf(b, sizeof(b), argv[1], "bios");
    snprintf(cmd, sizeof(cmd), "%s", b);
    system(cmd);
    sleep(1);
    return 0;

#if	0
    prompt = "\nEnter \"bios\" to start PDOS.\nprompt>";
    write(STDOUT_FILENO, prompt, strlen(prompt));
    prompt = NULL;
    old_mode = fcntl(STDIN_FILENO, F_GETFL);
    new_mode = old_mode | O_NONBLOCK;
    fcntl(STDIN_FILENO, F_SETFL, new_mode);

    //printenv();

    while (1) {
        char buf[1024];
        ssize_t l;
        int i;
        l = read(fd, buf, sizeof(buf));
        if (l > 0) {
            write(STDOUT_FILENO, buf, l);
            i = 0;
            while (i < l) {
                if (buf[i] == '\n') {
                    cmd[n] = '\0';
                    if (n > 0) {
                        if (!strcmp(cmd, "exit")) {
                            exit(0);
                        } else if (!strcmp(cmd, "test1")) {
                            prompt = "\x1b[A\r\x1b[K\x1b[1;32mopened \x1b[1;4;34m%s\x1b[0;1;32m in your browser.\x1b[0m\n\x1b[1;1H";
                            write(STDOUT_FILENO, prompt, strlen(prompt));
                        } else if (!strcmp(cmd, "LS")) {
                            snprintf(cmd, sizeof(cmd), argv[1], "ls");
                            run(cmd, NULL/*argv[2]*/);
                        } else if (!strcmp(cmd, "bios")) {
                            snprintf(cmd, sizeof(cmd), argv[1], "bios");
                            run(cmd, NULL);
                        } else {
                            system(cmd);
                        }
                     }

                    prompt ="prompt>";
                    n = 0;
                } else if (buf[i] == 0x08) {
                	buf[n--] = '\0';
                } else {
                    if (n < (sizeof(cmd) - 2)) {
                        cmd[n] = buf[i];
                        n++;
                    } else {
                        n = 0;
                    }
                }
                i++;
            }
        } else {
            if (prompt != NULL) {
                write(STDOUT_FILENO, prompt, strlen(prompt));
                prompt = NULL;
            }
            usleep(20000);
        }
    }
    return 0;
#endif
}
