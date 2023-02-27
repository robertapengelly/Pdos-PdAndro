#include <stdio.h>
#include <unistd.h>
#include <dirent.h>
#include <stdlib.h>

void printenv(const char *dir)
{
    char **env;
    env = environ;
    while (env[0]) {
        printf("%s\n", env[0]);
        env++;
    }
    //unlink("");
}

void ls(const char *dir)
{
    struct dirent** list;
    int n;
    n = scandir(dir, &list,NULL, alphasort);
    if (n < 0) {
        printf("%s : not found or empty\n", dir);
    } else {
        printf("Directory : %s\n", dir);
        while (n) {
            n--;
            printf("%s\n", list[n]->d_name);
            free(list[n]);
        }
        free(list);
    }
}
int main(int argc, char *argv[])
{
    int c;

    if (argc > 1) {
        ls(argv[1]);
    } else {
        ls("./");
    }
    //c = getchar();
    //printf("You typed: '%c'\n", c);
    return 0;
}

