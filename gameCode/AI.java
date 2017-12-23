package qirkat;

import static qirkat.PieceColor.*;
import java.util.ArrayList;

/** A Player that computes its own moves.
 *  @author Mishri Daga
 */
class AI extends Player {

    /** Maximum minimax search depth before going to static evaluation. */
    private static final int MAX_DEPTH = 8;
    /** A position magnitude indicating a win (for white if positive, black
     *  if negative). */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 1;
    /** A magnitude greater than a normal value. */
    private static final int INFTY = Integer.MAX_VALUE;

    /** A new AI for GAME that will play MYCOLOR. */
    AI(Game game, PieceColor myColor) {
        super(game, myColor);
    }

    @Override
    Move myMove() {
        Main.startTiming();
        Move move;
        findMove();
        move = _lastFoundMove;
        System.out.printf("%s moves %s.\n", myColor(), move.toString());
        Main.endTiming();
        return move;
    }

    /** Return a move for me from the current position, assuming there
     *  is a move. */
    private Move findMove() {
        Board b = new Board(board());

        if (myColor() == WHITE) {
            findMove(b, MAX_DEPTH, true, 1, -INFTY, INFTY);
        } else {
            findMove(b, MAX_DEPTH, true, -1, -INFTY, INFTY);
        }
        return _lastFoundMove;
    }

    /** The move found by the last call to one of the ...FindMove methods
     *  below. */
    private Move _lastFoundMove;

    /** Find a move from position BOARD and return its value, recording
     *  the move found in _lastFoundMove iff SAVEMOVE. The move
     *  should have maximal value or have value > BETA if SENSE==1,
     *  and minimal value or value < ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels.  Searching at level 0 simply returns a static estimate
     *  of the board value and does not set _lastMoveFound. */
    private int findMove(Board board, int depth, boolean saveMove, int sense,
                         int alpha, int beta) {
        Move best = null;
        ArrayList<Move> possibilities = board.getMoves();
        int bestSoFar = sense * -INFTY;
        if (board.gameOver() && (board.whoseMove() == myColor())) {
            return -1 * sense * WINNING_VALUE;
        }
        if (sense == 1) {
            if (depth == 0) {
                return staticScore(board);
            } else {
                for (Move move : possibilities) {
                    board.makeMove(move, true);
                    int response = findMove(board, depth - 1,
                            false, -1, alpha, beta);
                    if (response > bestSoFar) {
                        if (saveMove) {
                            _lastFoundMove = move;
                            best = move;
                        }
                        bestSoFar = java.lang.Math.max(bestSoFar, response);
                        alpha = java.lang.Math.max(alpha, bestSoFar);
                        if (beta <= alpha) {
                            board.undo();
                            break;
                        }
                    }
                    board.undo();
                }
            }
        } else {
            if (depth == 0) {
                return staticScore(board);
            } else {
                for (Move move : possibilities) {
                    board.makeMove(move, true);
                    int response = findMove(board, depth - 1,
                            false, 1, alpha, beta);
                    if (response < bestSoFar) {
                        if (saveMove) {
                            _lastFoundMove = move;
                            best = move;
                        }
                        bestSoFar = java.lang.Math.min(bestSoFar, response);
                        beta = java.lang.Math.min(beta, bestSoFar);
                        if (beta <= alpha) {
                            board.undo();
                            break;
                        }
                    }
                    board.undo();
                }
            }
        }
        if (saveMove) {
            _lastFoundMove = best;
        }
        return bestSoFar;
    }
    /** Return a heuristic value for BOARD. */
    private int staticScore(Board board) {
        if (board.whoseMove() == myColor()) {
            ArrayList<Move> move = board.getMoves();
            if (move.size() == 0) {
                return -INFTY;
            }
            return move.size();
        } else {
            ArrayList<Move> move = board.getMoves();
            if (move.size() == 0) {
                return -INFTY;
            }
            return -move.size();
        }



    }

}
