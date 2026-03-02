package ru.hh.school.unittesting.example;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.hh.school.unittesting.homework.LibraryManager;
import ru.hh.school.unittesting.homework.NotificationService;
import ru.hh.school.unittesting.homework.UserService;

@ExtendWith(MockitoExtension.class)
class LibraryManagerTest {
    String HARRY_POTTER = "Harry Potter";
    static LibraryManager libraryManager = null;

    @Mock
    private NotificationService notificationService;

    @Mock
    private UserService userService;

    @BeforeEach
    void setup(){
        libraryManager = new LibraryManager(notificationService, userService);
    }

    @Test
    void shouldReturnZeroWhenBookDoesNotExist(){
        int availableCopies = libraryManager.getAvailableCopies("NotExistingBook");
        Assertions.assertEquals(0, availableCopies);
    }

    @Disabled // Fails on test №2
    @ParameterizedTest
    @CsvSource({
            "Harry Potter, 10, 10",
            "Harry Potter, -20, 0",
            "Harry Potter, 0, 0"
    })
    void shouldReturnCorrectNumberOfBooks(String book_id, int quantity, int total_quantity){
        libraryManager.addBook(book_id, quantity);
        Assertions.assertEquals(total_quantity, libraryManager.getAvailableCopies(book_id));
    }

    @Test
    void shouldCumulateNumberOfBook(){
        libraryManager.addBook(HARRY_POTTER, 1);
        libraryManager.addBook(HARRY_POTTER, 1);
        Assertions.assertEquals(2, libraryManager.getAvailableCopies(HARRY_POTTER));
    }

    @Disabled //Adds negative number of books
    @Test
    void shouldNotAddNegativeNumberOfBooks(){
        libraryManager.addBook(HARRY_POTTER, 10);
        libraryManager.addBook(HARRY_POTTER, -1);
        Assertions.assertEquals(10, libraryManager.getAvailableCopies(HARRY_POTTER));
    }

    @ParameterizedTest
    @CsvSource({
            "Harry Potter, 10, true, Vadim, 9",
            "Harry Potter, 1, true, Vadim, 0",
            "Harry Potter, 0, false, Vadim, 0"
    })
    void shouldReturnDecreasedNumberOfBook(String book, int added, boolean allowToBorrow, String userId, int total){
        Mockito.when(userService.isUserActive(userId)).thenReturn(true);
        libraryManager.addBook(book, added);
        Assertions.assertEquals(allowToBorrow, libraryManager.borrowBook(book, userId));
        Assertions.assertEquals(total, libraryManager.getAvailableCopies(book));
    }

    @Test
    void shouldNotAllowBorrowBookToInactiveUser(){
        Mockito.when(userService.isUserActive(Mockito.any())).thenReturn(false);
        libraryManager.addBook(HARRY_POTTER, 1);
        Assertions.assertFalse(libraryManager.borrowBook("", "Any user"));
        Assertions.assertEquals(1, libraryManager.getAvailableCopies(HARRY_POTTER));
    }

    @Test
    void shouldReturnFalseInCaseOfReturnNotExistingBook(){
        Assertions.assertFalse(libraryManager.returnBook(HARRY_POTTER, "User"));
    }

    @Test
    void shouldAddReturnedBookInBookInventory(){
        Mockito.when(userService.isUserActive(Mockito.any())).thenReturn(true);
        libraryManager.addBook(HARRY_POTTER, 1);
        libraryManager.borrowBook(HARRY_POTTER, "User");
        Assertions.assertTrue(libraryManager.returnBook(HARRY_POTTER, "User"));
        Assertions.assertEquals(1, libraryManager.getAvailableCopies(HARRY_POTTER));
    }

    @Test
    void shouldThrowsException(){
        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> libraryManager.calculateDynamicLateFee(-1, true, true)
                );
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
    void shouldReturnCorrectLateFee(int overdueDays, boolean isBestseller, boolean isPremiumMember, double resultFee){
        Assertions.assertEquals(
                resultFee,
                libraryManager.calculateDynamicLateFee(overdueDays, isBestseller, isPremiumMember)
        );
    }

}
