extern void show_user(char[]);

int main(int argc,char **argv)
{
    
	if (argv[1]) show_user(argv[1]);
	else show_user("/dev/tty1");
	exit(0);
}
