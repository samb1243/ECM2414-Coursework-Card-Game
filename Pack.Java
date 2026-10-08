import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Pack{

    public static final int CARDS_PER_PLAYER = 8;

    private final List<Card> cards;

    private Pack(List<Card> cards) {
        this.cards = cards;
    }

    public static Pack load(Path file, int players) throws InvalidPackException{
    
    }

    public List<Card> getCards() {  
        return this.cards;
    }

    

}