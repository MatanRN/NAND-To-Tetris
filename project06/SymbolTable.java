import java.util.HashMap;
import java.util.NoSuchElementException;

/**
 * This class represents a symbol table for a Hack assembly program.
 */
public class SymbolTable {
    private HashMap<String, Integer> table;

    /**
     * Constructs a new SymbolTable and initializes it with predefined symbols.
     */
    public SymbolTable() {
        this.table = new HashMap<>();
        initialize();
    }

    /**
     * Initializes the symbol table with predefined symbols.
     */
    private void initialize() {
        for (int i = 0; i < 16; i++) {
            this.table.put("R" + i, i);
        }
        this.table.put("SCREEN", 16384);
        this.table.put("KBD", 24576);
        this.table.put("SP", 0);
        this.table.put("LCL", 1);
        this.table.put("ARG", 2);
        this.table.put("THIS", 3);
        this.table.put("THAT", 4);
    }

    /**
     * Adds a new entry to the symbol table.
     * @param symbol the symbol to add
     * @param address the address associated with the symbol
     */
    public void addEntry(String symbol, int address) {
        this.table.put(symbol, address);
    }

    /**
     * Checks if the symbol table contains the given symbol.
     * @param symbol the symbol to check
     * @return true if the symbol table contains the symbol, false otherwise
     */
    public boolean contains(String symbol) {
        return this.table.containsKey(symbol);
    }

    /**
     * Returns the address associated with the given symbol.
     * @param symbol the symbol to look up
     * @return the address associated with the symbol
     * @throws NoSuchElementException if the symbol is not in the table
     */
    public int getAddress(String symbol) {
        if (!this.table.containsKey(symbol)) {
            throw new NoSuchElementException("Symbol not found in table: " + symbol);
        }
        return this.table.get(symbol);
    }
}
