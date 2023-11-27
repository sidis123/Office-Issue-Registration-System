package com.sourcery.oirs.service;

import com.sourcery.oirs.database.entity.IssueEntity;
import com.sourcery.oirs.database.entity.UserEntity;
import com.sourcery.oirs.database.repository.IssueRepository;
import com.sourcery.oirs.database.repository.OfficeRepository;
import com.sourcery.oirs.database.repository.UserRepository;
import com.sourcery.oirs.email.EmailService;
import com.sourcery.oirs.exceptions.BusyIssueNameException;
import com.sourcery.oirs.exceptions.IssueNotFoundException;
import com.sourcery.oirs.model.Issue;
import com.sourcery.oirs.model.IssueDetailRequestDto;
import com.sourcery.oirs.model.IssueDetailsResponseDto;
import com.sourcery.oirs.model.OfficeResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IssueService {
    private static final String ISSUE_NOT_FOUND = "Issue with %s id not found";

    private final IssueRepository issueRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public List<Issue> getAllIssue(int offset, int limit) {
        return issueRepository.findAllIssuesPage((offset - 1) * limit, limit);
    }
    private final OfficeRepository officeRepository;

    public List<Issue> getAllIssue() {
        return issueRepository.findAll();
    }


    public IssueDetailsResponseDto getIssueById(UUID id) {
        return issueRepository.findById(id)
                .orElseThrow(() -> new IssueNotFoundException(String.format(ISSUE_NOT_FOUND, id)));
    }


    public void deleteIssue(UUID id) {
        issueRepository.findIssue(id)
                .orElseThrow(() -> new IssueNotFoundException(String.format(ISSUE_NOT_FOUND, id)));
        issueRepository.delete(id);
    }
    public List<Issue> getIssuesByStatus(String status, int offset, int limit, UUID officeID, UUID employeeID) {
        // If no office ID is given it makes the request return issues from all offices
        boolean returnAllOffices = officeID == null;
        // If no employee ID is given it makes the request return issues from all employees
        boolean returnAllEmployees = employeeID == null;
        return issueRepository.findByStatusPage(status, (offset - 1) * limit, limit, officeID, employeeID, returnAllOffices, returnAllEmployees);
    }
    public List<Issue> getUserIssues(UUID id, int offset, int limit){ return issueRepository.findReportedByPage(id, (offset - 1) * limit, limit); }





    // When saving a new issue in the database, use this method to send a message to the office admins about new issue
    // Also need to create real admins emails in the database
    private void sendEmailToAdmins(Issue issue) {
        List<String> emailsOfAdmins = getAdminsEmailByOffice(issue.getOfficeId());
        String emailOfIssueCreator = SecurityContextHolder.getContext().getAuthentication().getName();
        String nameOfIssueCreator = userRepository.getUserNameByEmail(emailOfIssueCreator);
        String messageToAdmin = createIssueMessage(nameOfIssueCreator, emailOfIssueCreator, issue.getName(),
                issue.getDescription(), LocalDate.from(issue.getTime()));
        emailsOfAdmins.forEach(email -> emailService.sendEmail(email, issue.getName(), messageToAdmin));
    }

    private List<String> getAdminsEmailByOffice(UUID officeId) {
        return userRepository.getAdminsByOfficeId(officeId)
                .stream().map(UserEntity::getEmail).toList();
    }

    private String createIssueMessage (String employee,
                                       String email,
                                       String issueName,
                                       String description,
                                       LocalDate time) {
        return String.format("""
                New Issue: %s%n
                Created by %s%n
                Email: %s/%n
                Created at %s%n
                Issue description: %s""", issueName, employee, email, time, description);
    }

    public void updateIssue(IssueDetailRequestDto requestDto, UUID id) {
        Issue existingIssue = issueRepository.findIssue(id)
                .orElseThrow(() -> new IssueNotFoundException(String.format(ISSUE_NOT_FOUND, id)));
        existingIssue.setDescription(requestDto.getDescription());
        existingIssue.setOfficeId(requestDto.getOfficeId());
        existingIssue.setStatus(requestDto.getStatus());
        issueRepository.update(existingIssue);
    }

    public List<OfficeResponseDTO> getAllOffices() {
        return officeRepository.findAllOffices();
    }

    public void reportNewIssue (Issue issue) {
        Optional<IssueDetailsResponseDto> issueName = issueRepository.findByName(issue.getName());
        if (issueName.isPresent()){
            throw new BusyIssueNameException();
        }
        UUID officeId = issueRepository.getOfficeIdByName(issue.getName());
        issueRepository.insertIssue(
                IssueEntity.builder()
                        .id(UUID.randomUUID())
                        .name(issue.getName())
                        .status("open")
                        .description(issue.getDescription())
                        .commentCount(0.00)
                        .startTime(Timestamp.valueOf(LocalDateTime.now()))
                        .finishTime(null)
                        .employeeId(issue.getEmployeeId())
                        .officeId(issue.getOfficeId())
                        .rating(issue.getUpvoteCount())
                        .build()
        );
//        sendEmailToAdmins(issue);
    }

    public int getAllPageCount() {
        return issueRepository.findAll().size() / 10 + 1;
    }
    public int getStatusPageCount(String status){
        return issueRepository.findByStatus(status).size() / 10 + 1;
    }
    public int getUserPageCount(UUID id){
        return issueRepository.findReportedBy(id).size() / 10 + 1;
    }

}
