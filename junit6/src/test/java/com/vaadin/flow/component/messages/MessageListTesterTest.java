/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.component.messages;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.browserless.TreeOnFailureExtension;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.router.RouteConfiguration;

@ViewPackages
@ExtendWith(TreeOnFailureExtension.class)
class MessageListTesterTest extends BrowserlessTest {

    MessagesView view;

    @BeforeAll
    static void enableAttachmentsFeatureFlag() {
        System.setProperty("vaadin.experimental.messageListAttachments",
                "true");
    }

    @AfterAll
    static void clearAttachmentsFeatureFlag() {
        System.clearProperty("vaadin.experimental.messageListAttachments");
    }

    @BeforeEach
    void init() {
        RouteConfiguration.forApplicationScope()
                .setAnnotatedRoute(MessagesView.class);
        view = navigate(MessagesView.class);
    }

    @Test
    void size_returnsCorrectSize() {
        Assertions.assertEquals(3, test(view.list).size());

        addItem(view.list, new MessageListItem("Added message"));

        Assertions.assertEquals(4, test(view.list).size(),
                "Message should have been added to the list");
    }

    @Test
    void getMessages_allMessagesReturned() {
        Assertions.assertIterableEquals(
                Arrays.asList(view.one, view.two, view.three),
                test(view.list).getMessages());
    }

    @Test
    void getMessageByIndex_correctMessageReturned() {
        final MessageListTester<MessageList> list_ = test(view.list);
        Assertions.assertEquals(view.two, list_.getMessage(1));
        Assertions.assertEquals(view.three, list_.getMessage(2));
        Assertions.assertEquals(view.one, list_.getMessage(0));
    }

    @Test
    void getMessageFromTime_messagesAfterAreReturned() {
        final MessageListTester<MessageList> list_ = test(view.list);
        Assertions.assertIterableEquals(Arrays.asList(view.two, view.three),
                list_.getMessagesAfter(LocalDateTime.now().withHour(11)
                        .toInstant(ZoneOffset.UTC)));
        Assertions.assertIterableEquals(Arrays.asList(view.three),
                list_.getMessagesAfter(LocalDateTime.now().withHour(12)
                        .toInstant(ZoneOffset.UTC)));
    }

    @Test
    void getMessageUpToTime_messagesBeforeAreReturned() {
        final MessageListTester<MessageList> list_ = test(view.list);
        Assertions.assertIterableEquals(Arrays.asList(view.one, view.two),
                list_.getMessagesBefore(LocalDateTime.now().withHour(13)
                        .toInstant(ZoneOffset.UTC)));
        Assertions.assertIterableEquals(Arrays.asList(view.one),
                list_.getMessagesBefore(LocalDateTime.now().withHour(11)
                        .toInstant(ZoneOffset.UTC)));
    }

    @Test
    void getMessageBetweenTime_matchingMessagesReturned() {
        Assertions.assertIterableEquals(Arrays.asList(view.two),
                test(view.list).getMessages(
                        LocalDateTime.now().withHour(1)
                                .toInstant(ZoneOffset.UTC),
                        LocalDateTime.now().withHour(13)
                                .toInstant(ZoneOffset.UTC)));
    }

    @Test
    void getMessageForUser_messagesForUserReturned() {
        final MessageListTester<MessageList> list_ = test(view.list);
        Assertions.assertIterableEquals(Arrays.asList(view.one, view.three),
                list_.getMessages("Joe"));
        Assertions.assertIterableEquals(Arrays.asList(view.two),
                list_.getMessages("Jane"));

        Assertions.assertTrue(list_.getMessages(null).isEmpty());

        final MessageListItem nullUser = new MessageListItem("hi");
        addItem(view.list, nullUser);

        Assertions.assertIterableEquals(Arrays.asList(nullUser),
                list_.getMessages(null));
    }

    @Test
    void getAttachments_messageWithAttachments_attachmentsReturned() {
        final MessageListItem.Attachment report = new MessageListItem.Attachment(
                "report.pdf", "/files/report.pdf", "application/pdf");
        final MessageListItem.Attachment image = new MessageListItem.Attachment(
                "photo.png", "/files/photo.png", "image/png");
        view.one.setAttachments(Arrays.asList(report, image));

        Assertions.assertIterableEquals(Arrays.asList(report, image),
                test(view.list).getAttachments(0));
    }

    @Test
    void getAttachments_messageWithoutAttachments_emptyListReturned() {
        Assertions.assertTrue(test(view.list).getAttachments(1).isEmpty(),
                "Message without attachments should return an empty list");
    }

    @Test
    void getAttachments_indexOutOfBounds_throws() {
        Assertions.assertThrows(IndexOutOfBoundsException.class,
                () -> test(view.list).getAttachments(3));
        Assertions.assertThrows(IndexOutOfBoundsException.class,
                () -> test(view.list).getAttachments(-1));
    }

    @Test
    void getAttachmentByName_matchingAttachmentReturned() {
        final MessageListItem.Attachment report = new MessageListItem.Attachment(
                "report.pdf", "/files/report.pdf", "application/pdf");
        final MessageListItem.Attachment image = new MessageListItem.Attachment(
                "photo.png", "/files/photo.png", "image/png");
        view.two.setAttachments(Arrays.asList(report, image));

        final MessageListTester<MessageList> list_ = test(view.list);
        Assertions.assertEquals(report,
                list_.getAttachmentByName(1, "report.pdf"));
        Assertions.assertEquals(image,
                list_.getAttachmentByName(1, "photo.png"));
    }

    @Test
    void getAttachmentByName_noMatchingName_nullReturned() {
        view.one.setAttachments(List.of(new MessageListItem.Attachment(
                "report.pdf", "/files/report.pdf", "application/pdf")));

        final MessageListTester<MessageList> list_ = test(view.list);
        Assertions.assertNull(list_.getAttachmentByName(0, "missing.pdf"),
                "Unknown attachment name should return null");
        Assertions.assertNull(list_.getAttachmentByName(1, "report.pdf"),
                "Message without attachments should return null");
    }

    @Test
    void getAttachmentByName_indexOutOfBounds_throws() {
        Assertions.assertThrows(IndexOutOfBoundsException.class,
                () -> test(view.list).getAttachmentByName(3, "report.pdf"));
    }

    /**
     * Add a new message item to the MessageList.
     *
     * @param item
     *            item to add
     */
    private void addItem(MessageList list, MessageListItem item) {

        final ArrayList<MessageListItem> messageListItems = new ArrayList<>(
                list.getItems());
        messageListItems.add(item);
        list.setItems(messageListItems);
    }
}
