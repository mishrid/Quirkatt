package qirkat;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.Observable;
import java.util.Observer;
import java.util.Arrays;


import static qirkat.PieceColor.*;
import static qirkat.Move.*;

/** A Qirkat board.   The squares are labeled by column (a char value between
 *  'a' and 'e') and row (a char value between '1' and '5'.
 *
 *  For some purposes, it is useful to refer to squares using a single
 *  integer, which we call its "linearized index".  This is simply the
 *  number of the square in row-major order (with row 0 being the bottom row)
 *  counting from 0).
 *
 *  Moves on this board are denoted by Moves.
 *  @author Mishri Daga
 */
class Board extends Observable {

    /** A new, cleared board at the start of the game. */
    Board() {
        clear();
    }

    /** A copy of B. */
    Board(Board b) {
        internalCopy(b);
    }

    /** Return a constant view of me (allows any access method, but no
     *  method that modifies it). */
    Board constantView() {
        return this.new ConstantBoard();
    }

    /** Clear me to my starting state, with pieces in their initial
     *  positions. */
    void clear() {
        _whoseMove = WHITE;
        _gameOver = false;
        _allmoves.add(move(1, 1));
        _board = new ArrayList<>(Arrays.asList(EMPTY,
                WHITE, WHITE, WHITE, WHITE, WHITE,
                WHITE, WHITE, WHITE, WHITE, WHITE,
                BLACK, BLACK, EMPTY, WHITE, WHITE,
                BLACK, BLACK, BLACK, BLACK, BLACK,
                BLACK, BLACK, BLACK, BLACK, BLACK));
        setChanged();
        notifyObservers();
    }

    /** Copy B into me. */
    void copy(Board b) {
        internalCopy(b);
    }

    /** Copy B into me. */
    private void internalCopy(Board b) {
        _board = b._board;
        _whoseMove = b._whoseMove;
        _gameOver = b._gameOver;
        _allmoves = b._allmoves;
    }

    /** Set my contents as defined by STR.  STR consists of 25 characters,
     *  each of which is b, w, or -, optionally interspersed with whitespace.
     *  These give the contents of the Board in row-major order, starting
     *  with the bottom row (row 1) and left column (column a). All squares
     *  are initialized to allow horizontal movement in either direction.
     *  NEXTMOVE indicates whose move it is.
     */
    void setPieces(String str, PieceColor nextMove) {
        if (nextMove == EMPTY || nextMove == null) {
            throw new IllegalArgumentException("bad player color");
        }
        str = str.replaceAll("\\s", "");
        if (!str.matches("[bw-]{25}")) {
            throw new IllegalArgumentException("bad board description");
        }

        clear();


        for (int k = 0; k < str.length(); k += 1) {
            switch (str.charAt(k)) {
            case '-':
                set(k + 1, EMPTY);
                break;
            case 'b': case 'B':
                set(k + 1, BLACK);
                break;
            case 'w': case 'W':
                set(k + 1, WHITE);
                break;
            default:
                break;
            }
        }

        _whoseMove = nextMove;

        setChanged();
        notifyObservers();
    }

    /** Return true iff the game is over: i.e., if the current player has
     *  no moves. */
    boolean gameOver() {
        ArrayList<Move> moves = new ArrayList<>();
        PieceColor trueMove = _whoseMove;
        boolean flag = false;
        for (int k = 1; k <= MAX_INDEX; k++) {
            if (_board.get(k) == _whoseMove) {
                getMoves(moves, k);
                if (jumpPossible(k) || !moves.isEmpty()) {
                    flag = true;
                }
            }
        }
        _whoseMove = trueMove;
        return !flag;
    }

    /** Checks to see if the game is over regardless of whose turn,
     *  returns true if so.*/
    boolean totalGameOver() {
        if (_allmoves.size() < 3) {
            return false;
        }

        ArrayList<Move> moves = new ArrayList<>();
        PieceColor trueMove = _whoseMove;
        boolean flag = false;
        for (int k = 1; k <= MAX_INDEX; k++) {
            if (_board.get(k) == _whoseMove) {
                getMoves(moves, k);
                if (jumpPossible(k) || !moves.isEmpty()) {
                    flag = true;
                }
            }
        }

        _whoseMove = _whoseMove.opposite();
        boolean flag2 = false;
        for (int k = 1; k <= MAX_INDEX; k++) {
            if (_board.get(k) == _whoseMove) {
                getMoves(moves, k);
                if (jumpPossible(k) || !moves.isEmpty()) {
                    flag2 = true;
                }
            }
        }


        _whoseMove = trueMove;
        return !flag2;
    }




    /** Return the current contents of square C R, where 'a' <= C <= 'e',
     *  and '1' <= R <= '5'.  */
    PieceColor get(char c, char r) {
        assert validSquare(c, r);
        return get(index(c, r));
    }



    /** Return the current contents of the square at linearized index K. */
    PieceColor get(int k) {
        assert validSquare(k);
        return _board.get(k);
    }

    /** Set get(C, R) to V, where 'a' <= C <= 'e', and
     *  '1' <= R <= '5'. */
    private void set(char c, char r, PieceColor v) {
        assert validSquare(c, r);
        set(index(c, r), v);
    }

    /** Set get(K) to V, where K is the linearized index of a square. */
    private void set(int k, PieceColor v) {
        assert validSquare(k);
        _board.set(k, v);
    }

    /** Return true iff MOVE is legal on the current board. */
    public boolean legalMove(Move move) {
        boolean legality;
        if (move.isVestigial()) {
            return false;
        }
        if (!move.isJump()) {
            if (get(move.fromIndex()) != _whoseMove) {
                return false;
            }
            if (index(move.col0(), move.row0()) % 2 == 0) {
                return legalEvenNonCapturing(move);
            } else {
                return legalOddNonCapturing(move);
            }
        } else {
            if (index(move.col0(), move.row0()) % 2 == 0) {
                legality =  legalEvenCapturing(move);
            } else {
                legality = legalOddCapturing(move);
            }
            return legality;
        }
    }


    /** Returns whether or not MOVE is a legal non-capturing
     *  move from an even index. */
    boolean legalEvenNonCapturing(Move move) {
        if (!(get(move.col1(), move.row1()) == EMPTY)) {
            return false;
        }
        if (move.isLeft()) {
            if (_allmoves.get(_allmoves.size() - 2).isLeft()
                    || _allmoves.get(_allmoves.size() - 1).isLeft())  {
                Move move2 = _allmoves.get(_allmoves.size() - 2);
                Move move3 = _allmoves.get(_allmoves.size() - 1);
                if (move2.toIndex() == move.fromIndex()
                        && move2.fromIndex() == move.toIndex()
                        || move3.toIndex() == move.fromIndex()
                        && move3.fromIndex() == move3.toIndex()) {
                    return false;
                }
            }
        }
        if (move.isLeft()) {
            if (_whoseMove == BLACK) {
                if (move.toIndex() <= SIDE) {
                    return false;
                }
            } else {
                if (move.toIndex() > MAX_INDEX - SIDE) {
                    return false;
                }
            }
        }
        if (move.isRightMove()) {
            if (_allmoves.get(_allmoves.size() - 2).isLeft()
                    || _allmoves.get(_allmoves.size() - 1).isLeft()) {
                Move move2 = _allmoves.get(_allmoves.size() - 2);
                Move move3 = _allmoves.get(_allmoves.size() - 1);
                if (move2.toIndex() == move.fromIndex()
                        && move2.fromIndex() == move.toIndex()
                        || move3.toIndex() == move.fromIndex()
                        && move3.fromIndex() == move3.toIndex()) {
                    return false;
                }
            }
        }
        if (move.isRightMove()) {
            if (_whoseMove == BLACK) {
                if (move.toIndex() <= SIDE) {
                    return false;
                }
            } else {
                if (move.toIndex() > MAX_INDEX - SIDE) {
                    return false;
                }
            }
        }
        if (_whoseMove == WHITE) {
            return (move.isLeft() || move.isRightMove()
                    || move.isForwardMove());
        } else {
            return (move.isLeft() || move.isRightMove()
                    || move.isBackwardMove());
        }
    }

    /** Returns whether or not MOVE is a legal capturing move
     *  from an even index. */
    boolean legalEvenCapturing(Move move) {
        if (!(get(move.col1(), move.row1()) == EMPTY)) {
            return false;
        }
        if (_board.get(move.jumpedIndex()) != _whoseMove.opposite()) {
            return false;
        }
        return move.isEvenJump();
    }

    /** Returns whether or not MOVE is a legal non-capturing
     *  move from an odd index. */
    boolean legalOddNonCapturing(Move move) {
        if (!(get(move.col1(), move.row1()) == EMPTY)) {
            return false;
        }
        if (move.isLeft()) {
            if (_allmoves.get(_allmoves.size() - 2).isRightMove()
                    || _allmoves.get(_allmoves.size() - 1).isRightMove())  {
                Move move2 = _allmoves.get(_allmoves.size() - 2);
                Move move3 = _allmoves.get(_allmoves.size() - 1);
                if (move2.toIndex() == move.fromIndex()
                        && move2.fromIndex() == move.toIndex()
                        || move3.toIndex() == move.fromIndex()
                        && move3.fromIndex() == move3.toIndex()) {
                    return false;
                }
            }
        }
        if (move.isLeft()) {
            if (_whoseMove == BLACK) {
                if (move.toIndex() <= SIDE) {
                    return false;
                }
            } else {
                if (move.toIndex() > MAX_INDEX - SIDE) {
                    return false;
                }
            }
        }
        if (move.isRightMove()) {
            if (_allmoves.get(_allmoves.size() - 2).isLeft()
                    || _allmoves.get(_allmoves.size() - 1).isLeft()) {
                Move move2 = _allmoves.get(_allmoves.size() - 2);
                Move move3 = _allmoves.get(_allmoves.size() - 1);
                if (move2.toIndex() == move.fromIndex()
                        && move2.fromIndex() == move.toIndex()
                        || move3.toIndex() == move.fromIndex()
                        && move3.fromIndex() == move3.toIndex()) {
                    return false;
                }
            }
        }
        if (move.isRightMove()) {
            if (_whoseMove == BLACK) {
                if (move.toIndex() <= SIDE) {
                    return false;
                }
            } else {
                if (move.toIndex() > MAX_INDEX - SIDE) {
                    return false;
                }
            }
        }
        if (_whoseMove == WHITE) {
            return (move.isLeft() || move.isRightMove() || move.isForwardMove()
                    || move.isNorthEast() || move.isNorthWest());
        } else {
            return (move.isLeft() || move.isRightMove() || move.isBackwardMove()
                    || move.isSouthEast() || move.isSouthWest());
        }
    }

    /** Returns whether or not MOVE is a legal
     *  capturing move from an odd index. */
    boolean legalOddCapturing(Move move) {
        if (!(get(move.col1(), move.row1()) == EMPTY)) {
            return false;
        }
        if (_board.get(move.jumpedIndex()) != _whoseMove.opposite()) {
            return false;
        }
        return move.isOddJump();
    }



    /** Return a list of all legal moves from the current position. */
    ArrayList<Move> getMoves() {
        ArrayList<Move> result = new ArrayList<>();
        getMoves(result);
        result = moveContains(result);
        return result;
    }

    /** Return a list of all, both jumps and other
     *  legal moves from the current position. */
    ArrayList<Move> getAllMoves() {
        ArrayList<Move> result = new ArrayList<>();
        getAllMoves(result);
        result = moveContains(result);
        return result;
    }


    /** Add all legal moves from the current position to MOVES. */
    void getAllMoves(ArrayList<Move> moves) {
        if (jumpPossibleSTART()) {
            for (int k = 1; k <= MAX_INDEX; k += 1) {
                getJumps(moves, k);
            }
        }
        for (int k = 1; k <= MAX_INDEX; k += 1) {
            getMoves(moves, k);
        }

    }
    /** Add all legal moves from the current position to MOVES. */
    void getMoves(ArrayList<Move> moves) {
        if (jumpPossibleSTART()) {
            for (int k = 1; k <= MAX_INDEX; k += 1) {
                getJumps(moves, k);
            }
        }

        if (moves.size() == 0) {
            for (int k = 1; k <= MAX_INDEX; k += 1) {
                getMoves(moves, k);
            }
        }
    }

    /** Add all legal non-capturing moves from the position
     *  with linearized index K to MOVES. */
    private void getMoves(ArrayList<Move> moves, int k) {
        if (get(k) == _whoseMove.opposite() || get(k) == EMPTY) {
            return;
        }
        ArrayList<Move> potentialMoves = new ArrayList<>();
        if (k + 1 <= MAX_INDEX) {
            Move move1 = Move.move(k, k + 1);
            potentialMoves.add(move1);
        }
        if (k - 1 >= 1) {
            Move move2 = Move.move(k, k - 1);
            potentialMoves.add(move2);
        }
        if (_whoseMove == WHITE) {
            if (k + 5 <= MAX_INDEX) {
                Move move3 = Move.move(k, k + 5);
                potentialMoves.add(move3);
            }
        } else {
            if (k - 5 >= 1) {
                Move move4 = Move.move(k, k - 5);
                potentialMoves.add(move4);
            }
        }
        if (k % 2 == 1) {
            if (_whoseMove == WHITE) {
                if (k + 6 <= MAX_INDEX) {
                    Move move5 = Move.move(k, k + 6);
                    potentialMoves.add(move5);
                }
                if (k + 4 <= MAX_INDEX) {
                    Move move6 = Move.move(k, k + 4);
                    potentialMoves.add(move6);
                }
            } else {
                if (k - 6 >= 1) {
                    Move move7 = Move.move(k, k - 6);
                    potentialMoves.add(move7);
                }
                if (k - 4 >= 1) {
                    Move move8 = Move.move(k, k - 4);
                    potentialMoves.add(move8);
                }
            }
        }
        for (Move move : potentialMoves) {
            if (legalMove(move)) {
                moves.add(move);
            }
        }
    }

    /** Add all legal captures from the position with linearized index K
     *  to MOVES. */
    private void getJumps(ArrayList<Move> moves, int k) {
        if (get(k) != EMPTY && (jumpPossible(k))) {
            backtracking = new ArrayList<>(Arrays.asList(false, false, false,
                    false, false,
                    false, false, false, false, false,
                    false, false, false, false, false,
                    false, false, false, false, false,
                    false, false, false, false, false, false));
            getJumps(moves, k, move(k, k));
        }
    }

    /** Filters through RESULT which is a list of moves
     *  to make sure none are duplicates or extensions of others
     *  and returns new list of moves. */
    private ArrayList<Move> moveContains(ArrayList<Move> result) {
        for (int i = 1; i < result.size();) {
            Move move1 = result.get(i - 1);
            Move move2 = result.get(i);
            while (move2 != null) {
                if (move1.fromIndex() == move2.fromIndex()
                        && move1.toIndex() == move2.toIndex()) {
                    move2 = move2.hasNextJump();
                    move1 = move1.hasNextJump();
                } else {
                    i = i + 1;
                    break;
                }
            }
            if (move2 == null) {
                result.remove(i);
            }
        }
        return result;
    }


    /** Gets all jumps from index k and adds to list
     *  moves starting with a moveVestigial.
     *  @param moveVestigial is a move from position K
     *  @param k linearized index
     *  @param moves list of moves to add to*/
    private void getJumps(ArrayList<Move> moves, int k, Move moveVestigial) {
        ArrayList<Move> potentialMoves = getNextJump(k);
        if (potentialMoves.isEmpty()) {
            if (!moveVestigial.isVestigial()) {
                moves.add(moveVestigial);
                return;
            }
        }

        Move temp = moveVestigial;
        for (Move move : potentialMoves) {
            while (backtracking.get(move.finalJumpedIndex())) {
                if (potentialMoves.indexOf(move) + 1
                        <= potentialMoves.size() - 1) {
                    move = potentialMoves.get(potentialMoves.indexOf(move) + 1);
                } else {
                    moves.add(moveVestigial);
                    return;
                }
            }

            moveVestigial = move(moveVestigial, move);
            backtracking.set(moveVestigial.finalJumpedIndex(), true);
            getJumps(moves, moveVestigial.finalJumpIndex(), moveVestigial);
            backtracking = new ArrayList<>(Arrays.asList(false, false,
                    false, false, false,
                    false, false, false, false, false,
                    false, false, false, false, false,
                    false, false, false, false, false,
                    false, false, false, false, false, false));
            moveVestigial = temp;
            while (moveVestigial != null) {
                backtracking.set(moveVestigial.jumpedIndex(), true);
                moveVestigial = moveVestigial.hasNextJump();
            }
            moveVestigial = temp;
        }
    }
    /** Helper method that returns a list of moves
     * possible from a given square.
     * @param k is a linearized index. */
    private ArrayList<Move> getNextJump(int k) {
        ArrayList<Move> moves = new ArrayList<>();
        if (jumpPossible(k)) {
            if (jumpUp(k)) {
                moves.add(move(k, k + 10));
            }
            if (jumpDown(k)) {
                moves.add(move(k, k - 10));
            }
            if (jumpLeft(k)) {
                moves.add(move(k, k - 2));
            }
            if (jumpRight(k)) {
                moves.add(move(k, k + 2));
            }
            if (k % 2 == 1) {
                if (jumpNorthEast(k)) {
                    moves.add(move(k, k + 12));
                }
                if (jumpNorthWest(k)) {
                    moves.add(move(k, k + 8));
                }
                if (jumpSouthWest(k)) {
                    moves.add(move(k, k - 12));
                }
                if (jumpSouthEast(k)) {
                    moves.add(move(k, k - 8));
                }
            }
        }
        return moves;
    }

    /** Return true iff MOVE is a valid jump sequence on the current board.
     *  MOV must be a jump or null.  If ALLOWPARTIAL, allow jumps that
     *  could be continued and are valid as far as they go.  */
    boolean checkJump(Move move, boolean allowPartial) {
        if (move == null) {
            return true;
        }
        return legalMove(move);
    }

    /** Return true iff a jump is possible for a piece at position C R. */
    boolean jumpPossible(char c, char r) {
        return jumpPossible(index(c, r));
    }

    /** Return true iff a jump is possible for a piece at position with
     *  linearized index K. */
    boolean jumpPossible(int k) {
        if (_board.get(k) == _whoseMove.opposite()) {
            return false;
        }
        if (jumpUp(k) || jumpDown(k) || jumpRight(k) || jumpLeft(k)) {
            return true;
        }
        if (k % 2 == 1) {
            if (jumpNorthEast(k) || jumpNorthWest(k)
                    || jumpSouthWest(k) || jumpSouthEast(k)) {
                return true;
            }
        }

        return false;
    }

    /** Checks if there are any jumps possible from K.
     * @return whether their are jumps. */
    boolean jumpPossibleSTART(int k) {
        if (_board.get(k) == _whoseMove.opposite()
                || _board.get(k) == EMPTY) {
            return false;
        }
        if (jumpUp(k) || jumpDown(k) || jumpRight(k) || jumpLeft(k)) {
            return true;
        }
        if (k % 2 == 1) {
            if (jumpNorthEast(k) || jumpNorthWest(k)
                    || jumpSouthWest(k) || jumpSouthEast(k)) {
                return true;
            }
        }
        return false;
    }


    /** Return true iff a jump forward is possible for a piece at position with
     *  linearized index K. */
    boolean jumpUp(int k) {
        char c = col(k);
        char r = row(k);
        char rUp = (char) (r + 1);
        char rUpUp = (char) (rUp + 1);
        if (validSquare(c, rUp)) {
            if (get(c, rUp) == _whoseMove.opposite()) {
                if (validSquare(c, rUpUp)) {
                    if (get(c, rUpUp) == EMPTY) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Return true iff a jump backward is possible for a piece at position with
     *  linearized index K. */
    boolean jumpDown(int k) {
        char c = col(k);
        char r = row(k);
        char rDown = (char) (r - 1);
        char rDownDown = (char) (rDown - 1);
        if (validSquare(c, rDown)) {
            if (get(c, rDown) == _whoseMove.opposite()) {
                if (validSquare(c, rDownDown)) {
                    if (get(c, rDownDown) == EMPTY) {
                        return true;
                    }
                }

            }
        }

        return false;
    }

    /** Return true iff a jump left is possible for a piece at position with
     *  linearized index K. */
    boolean jumpLeft(int k) {
        char c = col(k);
        char r = row(k);
        char cLeft = (char) (c - 1);
        char cLeftLeft = (char) (cLeft - 1);
        if (validSquare(cLeft, r)) {
            if (get(cLeft, r) == _whoseMove.opposite()) {
                if (validSquare(cLeftLeft, r)) {
                    if (get(cLeftLeft, r) == EMPTY) {
                        return true;
                    }
                }

            }
        }
        return false;
    }

    /** Return true iff a jump right is possible for a piece at position with
     *  linearized index K. */
    boolean jumpRight(int k) {
        char c = col(k);
        char r = row(k);
        char cRight = (char) (c + 1);
        char cRightRight = (char) (cRight + 1);
        if (validSquare(cRight, r)) {
            if (get(cRight, r) == _whoseMove.opposite()) {
                if (validSquare(cRightRight, r)) {
                    if (get(cRightRight, r) == EMPTY) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Return true iff a jump northeast is possible for a piece
     *  at position with linearized index K. */
    boolean jumpNorthEast(int k) {
        char c = col(k);
        char r = row(k);
        char cRight = (char) (c + 1);
        char cRightRight = (char) (cRight + 1);
        char rRight = (char) (r + 1);
        char rRightRight = (char) (rRight + 1);

        if (validSquare(cRight, rRight)) {
            if (get(cRight, rRight) == _whoseMove.opposite()) {
                if (validSquare(cRightRight, rRightRight)) {
                    if (get(cRightRight, rRightRight) == EMPTY) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Return true iff a jump northwest is possible for a piece
     * at position with linearized index K. */
    boolean jumpNorthWest(int k) {
        char c = col(k);
        char r = row(k);
        char cLeft = (char) (c - 1);
        char cLeftLeft = (char) (cLeft - 1);
        char rRight = (char) (r + 1);
        char rRightRight = (char) (rRight + 1);

        if (validSquare(cLeft, rRight)) {
            if (get(cLeft, rRight) == _whoseMove.opposite()) {
                if (validSquare(cLeftLeft, rRightRight)) {
                    if (get(cLeftLeft, rRightRight) == EMPTY) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Return true iff a jump southwest is possible for a piece
     *  at position with linearized index K. */
    boolean jumpSouthWest(int k) {
        char c = col(k);
        char r = row(k);
        char cLeft = (char) (c - 1);
        char cLeftLeft = (char) (cLeft - 1);
        char rLeft = (char) (r - 1);
        char rLeftLeft = (char) (rLeft - 1);

        if (validSquare(cLeft, rLeft)) {
            if (get(cLeft, rLeft) == _whoseMove.opposite()) {
                if (validSquare(cLeftLeft, rLeftLeft)) {
                    if (get(cLeftLeft, rLeftLeft) == EMPTY) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Return true iff a jump southeast is possible for a piece
     *  at position with linearized index K. */
    boolean jumpSouthEast(int k) {
        char c = col(k);
        char r = row(k);
        char cLeft = (char) (c + 1);
        char cLeftLeft = (char) (cLeft + 1);
        char rRight = (char) (r - 1);
        char rRightRight = (char) (rRight - 1);

        if (validSquare(cLeft, rRight)) {
            if (get(cLeft, rRight) == _whoseMove.opposite()) {
                if (validSquare(cLeftLeft, rRightRight)) {
                    if (get(cLeftLeft, rRightRight) == EMPTY) {
                        return true;
                    }
                }
            }
        }
        return false;
    }


    /** Return true iff a jump is possible from the current board. */
    boolean jumpPossible() {
        for (int k = 1; k <= MAX_INDEX; k += 1) {
            if (jumpPossible(k)) {
                return true;
            }
        }
        return false;
    }

    /** Return true iff a jump is possible from the current board. */
    boolean jumpPossibleSTART() {
        for (int k = 1; k <= MAX_INDEX; k += 1) {
            if (jumpPossibleSTART(k)) {
                return true;
            }
        }
        return false;

    }


    /** Return the color of the player who has the next move.  The
     *  value is arbitrary if gameOver(). */
    PieceColor whoseMove() {
        return _whoseMove;
    }

    /** Perform the move C0R0-C1R1, or pass if C0 is '-'.  For moves
     *  other than pass, assumes that legalMove(C0, R0, C1, R1). */
    void makeMove(char c0, char r0, char c1, char r1) {
        makeMove(Move.move(c0, r0, c1, r1, null), true);
    }

    /** Make the multi-jump C0 R0-C1 R1..., where NEXT is C1R1....
     *  Assumes the result is legal. */
    void makeMove(char c0, char r0, char c1, char r1, Move next) {
        makeMove(Move.move(c0, r0, c1, r1, next), true);
    }

    /** Make the Move MOV on this Board, assuming it is legal. START
     * indicates whether or not this is a continuation of a previous
     * jump move. */
    void makeMove(Move mov, boolean start) {
        if (!legalMove(mov)) {
            return;
        }
        if (start) {
            _allmoves.add(mov);
        }
        set(mov.col0(), mov.row0(), EMPTY);
        if (!mov.isJump()) {
            set(mov.col1(), mov.row1(), _whoseMove);
            setChanged();
            notifyObservers();
            _whoseMove = _whoseMove.opposite();
            return;
        } else {
            set(mov.col1(), mov.row1(), _whoseMove);
            set(mov.jumpedIndex(), EMPTY);
            if (mov.hasNextJump() == null) {
                setChanged();
                notifyObservers();
                _whoseMove = _whoseMove.opposite();
                return;
            } else {
                makeMove(mov.hasNextJump(), false);
            }
        }
    }


    /** Undo the last move, if any. */
    void undo() {
        _lastMove = _allmoves.remove(_allmoves.size() - 1);
        if (_lastMove == null) {
            return;
        }
        PieceColor pastPlayer = _whoseMove.opposite();
        if (!_lastMove.isJump()) {
            set(_lastMove.col1(), _lastMove.row1(), EMPTY);
            set(_lastMove.col0(), _lastMove.row0(), pastPlayer);
        } else {
            set(_lastMove.col0(), _lastMove.row0(), pastPlayer);
            set(_lastMove.col1(), _lastMove.row1(), EMPTY);
            set(_lastMove.jumpedIndex(), pastPlayer.opposite());

            while (_lastMove.hasNextJump() != null) {
                _lastMove = _lastMove.hasNextJump();
                set(_lastMove.col1(), _lastMove.row1(), EMPTY);
                set(_lastMove.jumpedIndex(), pastPlayer.opposite());
            }
        }

        _whoseMove = _whoseMove.opposite();
        setChanged();
        notifyObservers();
    }

    @Override
    public String toString() {
        return toString(false);
    }

    /** Return a text depiction of the board.  If LEGEND, supply row and
     *  column numbers around the edges. */
    String toString(boolean legend) {
        Formatter out = new Formatter();
        toString(out, legend);
        return out.toString();
    }

    /** Return a text depiction of the board.  If LEGEND, supply row and
     *  column numbers around the edges using OUT as the formatter.  */
    private void toString(Formatter out, boolean legend) {
        String row5 = " ";
        String row4 = " ";
        String row3 = " ";
        String row2 = " ";
        String row1 = " ";
        String board = "";

        for (PieceColor piece : _board) {
            if (piece == BLACK) {
                board = board + "b ";
            }
            if (piece == WHITE) {
                board = board + "w ";
            }
            if (piece == EMPTY) {
                board = board + "- ";
            }
        }

        String equals = "===";
        int one = 1;
        int ten = 10;

        board = board.substring(one, ten + ten + ten + ten + ten + one);
        row1 = " " + board.substring(one - one, ten);
        row2 = " " + board.substring(ten, ten + ten);
        row3 = " " + board.substring(ten + ten, ten + ten + ten);
        row4 = " " + board.substring(ten + ten + ten, ten + ten + ten + ten);
        row5 = " " + board.substring(ten + ten + ten + ten,
                ten + ten + ten + ten + ten);

        if (legend) {
            row1 = "1" + row1;
            row2 = "2" + row2;
            row3 = "3" + row3;
            row4 = "4" + row4;
            row5 = "5" + row5;
            String row6 = "   a b c d e";
            out.format("%s\n%s\n%s\n%s\n%s\n%s", row5, row4,
                    row3, row2, row1, row6);
        } else {
            out.format("%s\n%s\n%s\n%s\n%s\n%s\n%s", equals, row5,
                    row4, row3, row2, row1, equals);
        }



    }

    /** Return true iff there is a move for the current player. */
    private boolean isMove() {
        return false;
    }


    /** Stores the last move made in the game. */
    private Move _lastMove;


    /** Set true when game ends. */
    private boolean _gameOver;

    /** Convenience value giving values of pieces at each ordinal position. */
    static final PieceColor[] PIECE_VALUES = PieceColor.values();

    /** One cannot create arrays of ArrayList<Move>, so we introduce
     *  a specialized private list type for this purpose. */
    private static class MoveList extends ArrayList<Move> {
    }

    /** A read-only view of a Board. */
    private class ConstantBoard extends Board implements Observer {
        /** A constant view of this Board. */
        ConstantBoard() {
            super(Board.this);
            Board.this.addObserver(this);
        }

        @Override
        void copy(Board b) {
            assert false;
        }

        @Override
        void clear() {
            assert false;
        }

        @Override
        void makeMove(Move move, boolean start) {
            assert false;
        }

        /** Undo the last move. */
        @Override
        void undo() {
            assert false;
        }

        @Override
        public void update(Observable obs, Object arg) {
            super.copy((Board) obs);
            setChanged();
            notifyObservers(arg);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Board) {
            Board b = (Board) o;
            return _board == b._board;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    /** A list containing all the PieceColors of the board. */
    private ArrayList<PieceColor> _board;

    /** A list containing all past moves. */
    private ArrayList<Move> _allmoves = new ArrayList<>();

    /** Player that is on move. */
    private PieceColor _whoseMove;

    /** A list containing all past jumped
     * squares marked as true. */
    private ArrayList<Boolean> backtracking;

}
