package ru.hh.school.unittesting.homework;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LibraryManagerTest {
    private static final String HARRY_POTTER = "Harry Potter";

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserService userService;

    @InjectMocks
    private LibraryManager libraryManager;

    @Test
    void shouldReturnZeroWhenBookDoesNotExist() {
        int availableCopies = libraryManager.getAvailableCopies("NotExistingBook");
        Assertions.assertEquals(0, availableCopies);
    }

    @Disabled("Add negative quantity of new book")
    @ParameterizedTest
    @CsvSource({
            "Harry Potter, 10, 10",
            "Harry Potter, -20, 0",
            "Harry Potter, 0, 0"
    })
    void shouldReturnCorrectNumberOfBooks(String bookId, int quantity, int totalQuantity) {
        libraryManager.addBook(bookId, quantity);
        Assertions.assertEquals(totalQuantity, libraryManager.getAvailableCopies(bookId));
    }

    @Test
    void shouldCumulateNumberOfBook() {
        libraryManager.addBook(HARRY_POTTER, 1);
        libraryManager.addBook(HARRY_POTTER, 1);
        Assertions.assertEquals(2, libraryManager.getAvailableCopies(HARRY_POTTER));
    }

    @Disabled("The method should not add a negative number of books, as this may violate the logic of the borrowBook method. " +
            "If this functionality is required, then the decreaseBook method should be created. (Probably :) )")
    @Test
    void shouldNotAddNegativeNumberOfBooks() {
        libraryManager.addBook(HARRY_POTTER, 10);
        libraryManager.addBook(HARRY_POTTER, -1);
        Assertions.assertEquals(10, libraryManager.getAvailableCopies(HARRY_POTTER));
    }

    @Test
    void shouldReturnDecreasedNumberOfBook() {
        Mockito.when(userService.isUserActive("userId")).thenReturn(true);
        libraryManager.addBook(HARRY_POTTER, 10);
        Assertions.assertTrue(libraryManager.borrowBook(HARRY_POTTER, "userId"));
        Assertions.assertEquals(9, libraryManager.getAvailableCopies(HARRY_POTTER));
    }

    @Test
    void shouldNotAllowBorrowUnavailableBook() {
        Mockito.when(userService.isUserActive(Mockito.any())).thenReturn(true);
        libraryManager.addBook(HARRY_POTTER, 0);
        Assertions.assertFalse(libraryManager.borrowBook(HARRY_POTTER, "userId"));
        Assertions.assertEquals(0, libraryManager.getAvailableCopies(HARRY_POTTER));
    }

    @Test
    void shouldNotAllowBorrowBookToInactiveUser() {
        Mockito.when(userService.isUserActive(Mockito.any())).thenReturn(false);
        libraryManager.addBook(HARRY_POTTER, 1);
        Assertions.assertFalse(libraryManager.borrowBook("", "Any user"));
        Assertions.assertEquals(1, libraryManager.getAvailableCopies(HARRY_POTTER));
    }

    @Test
    void shouldReturnFalseInCaseOfReturnNotExistingBook() {
        Assertions.assertFalse(libraryManager.returnBook(HARRY_POTTER, "User"));
    }

    @Test
    void shouldReturnFalseWhenUserDidNotBorrow() {
        libraryManager.addBook(HARRY_POTTER, 2);
        libraryManager.borrowBook(HARRY_POTTER, "Vadim");
        Assertions.assertFalse(libraryManager.returnBook(HARRY_POTTER, "KVadim"));
    }

    @Disabled("We should be able to give and take back similar books from different users")
    @Test
    void twoPersonsBorrowSimilarBook() {
        Mockito.when(userService.isUserActive(Mockito.any())).thenReturn(true);
        libraryManager.addBook(HARRY_POTTER, 2);
        Assertions.assertTrue(libraryManager.borrowBook(HARRY_POTTER, "Vadim"));
        Assertions.assertTrue(libraryManager.borrowBook(HARRY_POTTER, "KVadim"));
        Assertions.assertTrue(libraryManager.returnBook(HARRY_POTTER, "Vadim"));
        Assertions.assertTrue(libraryManager.returnBook(HARRY_POTTER, "KVadim"));
    }

    @Test
    void shouldAddReturnedBookInBookInventory() {
        Mockito.when(userService.isUserActive(Mockito.any())).thenReturn(true);
        libraryManager.addBook(HARRY_POTTER, 1);
        libraryManager.borrowBook(HARRY_POTTER, "User");
        Assertions.assertTrue(libraryManager.returnBook(HARRY_POTTER, "User"));
        Assertions.assertEquals(1, libraryManager.getAvailableCopies(HARRY_POTTER));
    }

    @Test
    void shouldThrowsException() {
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> libraryManager.calculateDynamicLateFee(-1, true, true)
        );
    }

    @Test
    void notificationDueToInactiveAccount() {
        Mockito.when(userService.isUserActive(Mockito.any())).thenReturn(false);
        libraryManager.borrowBook(HARRY_POTTER, "user");
        Mockito.verify(notificationService, Mockito.times(1))
                .notifyUser("user", "Your account is not active.");
    }

    @Test
    void notifyUserInCaseOfSuccessfulBorrow() {
        Mockito.when(userService.isUserActive(Mockito.any())).thenReturn(true);
        libraryManager.addBook(HARRY_POTTER, 1);
        libraryManager.borrowBook(HARRY_POTTER, "user");
        Mockito.verify(notificationService, Mockito.times(1))
                .notifyUser("user", "You have borrowed the book: " + HARRY_POTTER);
    }

    @Test
    void notifyUserInCaseOfReturningABook() {
        Mockito.when(userService.isUserActive(Mockito.any())).thenReturn(true);
        libraryManager.addBook(HARRY_POTTER, 1);
        libraryManager.borrowBook(HARRY_POTTER, "user");
        libraryManager.returnBook(HARRY_POTTER, "user");
        Mockito.verify(notificationService, Mockito.times(1))
                .notifyUser("user", "You have returned the book: " + HARRY_POTTER);
    }

    @ParameterizedTest
    @CsvSource({
            "0, false, false, 0",
            "0, true, true, 0",
            "1, false, false, 0.5",
            "1, true, false, 0.75",
            "1, false, true, 0.4",
            "1, true, true, 0.6",
            "2147483647, false, false, 1073741823.5",
            "2147483647, true, true, 1288490188.2"
    })
    void shouldReturnCorrectLateFee(int overdueDays, boolean isBestseller, boolean isPremiumMember, double resultFee) {
        Assertions.assertEquals(
                resultFee,
                libraryManager.calculateDynamicLateFee(overdueDays, isBestseller, isPremiumMember)
        );
    }

}
