import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GeneratePasswordHash {
    public static void main(String[] args) {
        if (args.length == 0) {
            throw new IllegalArgumentException("Please provide a password");
        }

        System.out.println(new BCryptPasswordEncoder().encode(args[0]));
    }
}